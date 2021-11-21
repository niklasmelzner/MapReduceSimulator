package com.melzner.mapreduce.cluster;

import com.melzner.mapreduce.cluster.Cluster.ClusterEventType;
import com.melzner.mapreduce.scenario.ClusterConfig;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Predicate;

public class DFS {

    public static final int COPY_COUNT = 3;
    private final PriorityQueue<Machine> machinesByDriveUsage = new PriorityQueue<>(Comparator.comparingLong(o -> o.hardDrive.driveUsage));
    private final Cluster cluster;
    private int currentFileId;

    public DFS(Cluster cluster) {
        this.cluster = cluster;
        machinesByDriveUsage.addAll(cluster.getMachines());
    }

    public DFSFile writeFile(long size) {
        return new DFSFile(currentFileId++, size, cluster.configuration);
    }

    public int newFileId() {
        return currentFileId++;
    }

    public class DFSFile {

        private final int fileId;
        /** stores [size, machine1, machine2, machine3] for each block */
        long[][] blocks;
        long blockSize;

        DFSFile(int fileId, long size, ClusterConfig configuration) {
            this.fileId = fileId;
            blockSize = configuration.dfsBlockSize.get();
            int blockCount = (int) (size / blockSize + (size % blockSize != 0 ? 1 : 0));
            blocks = new long[blockCount][];

            for (int i = 0; i < blocks.length; i++) {
                long[] block = new long[COPY_COUNT + 1];
                block[0] = blockSize;
                for (int j = 0; j < COPY_COUNT; j++) {
                    block[j + 1] = writeBlockOnNewMachine(i, blockSize);
                }
                blocks[i] = block;
            }

            cluster.addListener(ClusterEventType.MACHINE_FAILED, e -> handleMachineFail(e.machine));
        }

        public int getFileId() {
            return fileId;
        }

        private int writeBlockOnNewMachine(int index, long blockSize) {
            Machine machine = machinesByDriveUsage.poll();
            //noinspection ConstantConditions
            machine.hardDrive.newBlock(fileId, index, blockSize);
            machinesByDriveUsage.add(machine);
            return machine.getId();
        }

        private void handleMachineFail(Machine machine) {
            machinesByDriveUsage.remove(machine);
            int id = machine.getId();
            for (int iBlock = 0; iBlock < blocks.length; iBlock++) {
                long[] block = blocks[iBlock];
                for (int i = 1; i < block.length; i++) {
                    if (block[i] == id) {
                        block[i] = writeBlockOnNewMachine(iBlock, block[0]);
                    }
                }
            }
        }

        public int getBlockCount() {
            return blocks.length;
        }

        public Machine getFreeMachineForBlock(int index) {
            return getFreeMachineForBlock(index, m -> true);
        }

        public Machine getFreeMachineForBlock(int index, Predicate<Machine> filter) {
            long[] block = blocks[index];
            for (int i = 1; i < block.length; i++) {
                Machine machine = cluster.getMachine((int) block[i]);
                if (! machine.isBusy() && machine.isAlive() && filter.test(machine)) {
                    return machine;
                }
            }
            return null;
        }
    }
}
