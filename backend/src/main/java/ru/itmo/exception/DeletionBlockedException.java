package ru.itmo.exception;

import java.util.List;

public class DeletionBlockedException extends RuntimeException {
    private final String entity;
    private final Long id;
    private final long usageCount;
    private final List<Long> blockingCityIds;

    public String getEntity() {
        return entity;
    }

    public Long getId() {
        return id;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public List<Long> getBlockingCityIds() {
        return blockingCityIds;
    }

    public DeletionBlockedException(String entity, Long id, long usageCount, List<Long> blockingCityIds) {
        super(entity + " id=" + id + " is referenced by " + usageCount + " cities: " + blockingCityIds);
        this.entity = entity;
        this.id = id;
        this.usageCount = usageCount;
        this.blockingCityIds = blockingCityIds;
    }


}