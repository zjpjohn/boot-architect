package com.cloud.arch.transaction.core;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Getter
public class AsyncTxVersion implements Comparable<AsyncTxVersion> {

    private final String    version;
    private final Integer[] cells;

    public AsyncTxVersion(String version) {
        if (!StringUtils.hasText(version)) {
            throw new IllegalArgumentException("无效的版本参数，版本格式为'x.x.x'.");
        }
        this.version = version;
        String[] segments = version.split("\\.");
        cells = new Integer[segments.length];
        for (int i = 0; i < segments.length; i++) {
            this.cells[i] = Integer.parseInt(segments[i]);
        }
    }

    @Override
    public int compareTo(AsyncTxVersion other) {
        Integer[] otherCells = other.getCells();
        int       maxLength  = Math.max(this.cells.length, otherCells.length);
        for (int i = 0; i < maxLength; i++) {
            int v1    = i < this.cells.length ? this.cells[i] : 0;
            int v2    = i < otherCells.length ? otherCells[i] : 0;
            int delta = v1 - v2;
            if (delta == 0) {
                continue;
            }
            return delta > 0 ? 1 : -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AsyncTxVersion that = (AsyncTxVersion) o;
        return Arrays.equals(cells, that.cells);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(cells);
    }

}
