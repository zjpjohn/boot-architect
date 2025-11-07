package com.cloud.arch.web.custom;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Getter
public class RequestVersion implements Comparable<RequestVersion> {

    private final Integer[] cells;

    public RequestVersion(String version) {
        if (!StringUtils.hasText(version)) {
            throw new IllegalArgumentException("illegal version params，version pattern must be 'x.x.x'.");
        }
        String[] segments = version.split("\\.");
        cells = new Integer[segments.length];
        for (int i = 0; i < segments.length; i++) {
            this.cells[i] = Integer.parseInt(segments[i]);
        }
    }

    @Override
    public int compareTo(RequestVersion other) {
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
        RequestVersion that = (RequestVersion) o;
        return Arrays.equals(cells, that.cells);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(cells);
    }
}
