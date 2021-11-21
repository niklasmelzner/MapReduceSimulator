package com.melzner.mapreduce.cluster;

import com.melzner.mapreduce.scenario.ClusterConfig;

import java.util.*;

public class HardDrive {

    private final long maxSingleDriveSize;
    private final Map<Integer, BlockCluster> blocksByFile = new HashMap<>();
    long driveUsage;

    HardDrive(ClusterConfig configuration) {
        maxSingleDriveSize = configuration.maxSingleDiskSize.get();
    }

    public long getCapacity() {
        return maxSingleDriveSize;
    }

    public long getUsage() {
        return driveUsage;
    }

    public void newBlock(int fileId, int blockId, long blockSize) {
        if (driveUsage + blockSize > maxSingleDriveSize) {
            throw new HardDriveOverflowException();
        }
        blocksByFile.computeIfAbsent(fileId, i -> new BlockCluster()).addBlock(blockId, blockSize);
        driveUsage = driveUsage + blockSize;
    }

    public Set<Integer> getBlocksForFile(DFS.DFSFile file) {
        BlockCluster cluster = blocksByFile.get(file.getFileId());
        if (cluster == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(cluster.blocks);
        }
    }

    private static class BlockCluster {

        long size;
        Set<Integer> blocks = new HashSet<>();

        public void addBlock(int blockId, long blockSize) {
            size += blockSize;
            blocks.add(blockId);
        }
    }
}
