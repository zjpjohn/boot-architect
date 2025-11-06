package com.cloud.arch.mutex;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class MutexOwnerRepository implements IMutexOwnerRepository {

    private static final String INIT_MUTEX_SQL     =
            "insert ignore into arch_mutex(mutex,acquired_at,ttl_at,transition_at,owner_id,version)"
            + " values(?,0,0,0,'',0)";
    private static final String QUERY_MUTEX_SQL    =
            "select acquired_at,ttl_at,transition_at,owner_id,version,cast(unix_timestamp(current_timestamp(3)) * 1000 as unsigned) as current_at"
            + " from arch_mutex" + " where mutex=?";
    private static final String ACQUIRED_MUTEX_SQL = "update arch_mutex set"
                                                     + "  acquired_at=cast(unix_timestamp(current_timestamp(3))*1000 as unsigned),"
                                                     + "  ttl_at=(cast(unix_timestamp(current_timestamp(3))*1000 as unsigned)+?),"
                                                     + "  transition_at=(cast(unix_timestamp(current_timestamp(3))*1000 as unsigned)+?),"
                                                     + "  owner_id=?," + "  version=version+1" + "  where "
                                                     + "  mutex=?" + "  and ( "
                                                     + "    (transition_at < (unix_timestamp(current_timestamp(3))*1000))"
                                                     + "    or"
                                                     + "    (owner_id=? and transition_at > (unix_timestamp(current_timestamp(3))*1000))"
                                                     + "  );";
    private static final String RELEASE_MUTEX_SQL  = "update arch_mutex set"
                                                     + " acquired_at=0,ttl_at=0,transition_at=0,owner_id='',version=version+1"
                                                     + " where mutex=? and owner_id=?";

    private final DataSource dataSource;

    public MutexOwnerRepository(DataSource dataSource) {
        Preconditions.checkNotNull(dataSource, "dataSource must not be null.");
        this.dataSource = dataSource;
    }


    @Override
    public boolean initMutex(String mutex) {
        try (Connection connection = dataSource.getConnection()) {
            return this.initMutex(connection, mutex);
        } catch (SQLException exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    private boolean initMutex(Connection connection, String mutex) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INIT_MUTEX_SQL)) {
            statement.setString(1, mutex);
            return statement.executeUpdate() > 0;
        }
    }


    @Override
    public MutexOwnerEntity getOwner(String mutex) {
        try (Connection connection = dataSource.getConnection()) {
            return getOwner(connection, mutex);
        } catch (SQLException exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    private MutexOwnerEntity getOwner(Connection connection, String mutex) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(QUERY_MUTEX_SQL)) {
            statement.setString(1, mutex);
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundMutexException(Strings.lenientFormat(
                            "not found mutex[%s],please check configuration.",
                            mutex));
                }
                final long   acquiredAt   = resultSet.getLong(1);
                final long   ttlAt        = resultSet.getLong(2);
                final long   transitionAt = resultSet.getLong(3);
                final String ownerId      = resultSet.getString(4);
                final int    version      = resultSet.getInt(5);
                final long   currentAt    = resultSet.getLong(6);
                final MutexOwnerEntity entity = new MutexOwnerEntity(mutex, ownerId, acquiredAt, ttlAt, transitionAt);
                entity.setVersion(version);
                entity.setCurrentAt(currentAt);
                return entity;
            }
        }
    }

    @Override
    public boolean acquire(String mutex, String contenderId, long ttl, long transition) {
        try (Connection connection = dataSource.getConnection()) {
            return acquire(connection, mutex, contenderId, ttl, transition);
        } catch (SQLException exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    private boolean acquire(Connection connection, String mutex, String contenderId, long ttl, long transition)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(ACQUIRED_MUTEX_SQL)) {
            statement.setLong(1, ttl);
            statement.setLong(2, ttl + transition);
            statement.setString(3, contenderId);
            statement.setString(4, mutex);
            statement.setString(5, contenderId);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public MutexOwnerEntity acquireAndGetOwner(String mutex, String contenderId, long ttl, long transition) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                final boolean          acquired   = acquire(connection, mutex, contenderId, ttl, transition);
                final MutexOwnerEntity mutexOwner = getOwner(connection, mutex);
                //竞争到资源但是最新的资源持有者不是当前在竞争者
                if (acquired && !mutexOwner.isOwner(contenderId)) {
                    throw new IllegalStateException(Strings.lenientFormat(
                            "Contender:[%s] has acquired leadership, but MutexOwner status is inconsistent!",
                            contenderId));
                }
                connection.commit();
                return mutexOwner;
            } catch (Exception error) {
                connection.rollback();
                throw new RuntimeException(error.getMessage(), error);
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }


    @Override
    public boolean release(String mutex, String contenderId) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(RELEASE_MUTEX_SQL)) {
                statement.setString(1, mutex);
                statement.setString(2, contenderId);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

}
