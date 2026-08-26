package org.heather.hardlands.core.data;

public final class BoundedCounter {
    private final int limit;
    private int count;

    public BoundedCounter(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }

        this.limit = limit;
    }

    public int getLimit() {
        return this.limit;
    }

    public int getCount() {
        return this.count;
    }

    public boolean tryAdvance() {
        if (this.count >= this.limit) {
            return false;
        }

        this.count++;
        return true;
    }

    public boolean isAtLimit() {
        return this.count >= this.limit;
    }

    public void reset() {
        this.count = 0;
    }
}
