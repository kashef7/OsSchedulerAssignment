import java.util.*;

public class AgScheduler {
    private LinkedList<Process> readyQueue = new LinkedList<>();
    private List<Process> finishedProcesses = new ArrayList<>();
    private List<String> executionOrder = new ArrayList<>();

    public void schedule(List<Process> processes) {
        int currentTime = 0;
        Process currentProcess = null;
        int totalProcesses = processes.size();

        List<Process> pool = new ArrayList<>(processes);
        pool.sort(Comparator.comparingInt(Process::getArrivalTime));

        while (finishedProcesses.size() < totalProcesses) {
            // 1. Arrival Check
            checkArrivals(pool, currentTime);

            // 2. Pick next process if CPU is idle
            if (currentProcess == null && !readyQueue.isEmpty()) {
                currentProcess = readyQueue.poll();
                if (executionOrder.isEmpty() || !executionOrder.get(executionOrder.size()-1).equals(currentProcess.getName())) {
                    executionOrder.add(currentProcess.getName());
                }
            }

            if (currentProcess != null) {
                // Execute for 1 unit
                currentProcess.reduceRemaining();
                currentProcess.incrementUsed();
                currentTime++;

                // Thresholds
                int q = currentProcess.getQuantum();
                int q25 = (int) Math.ceil(0.25 * q);
                int q50 = (int) Math.ceil(0.50 * q);
                int consumed = currentProcess.getCurrentQuantumUsed();

                // Check for Arrivals AGAIN because time moved
                checkArrivals(pool, currentTime);

                // --- SCENARIO IV: Finished ---
                if (currentProcess.isFinished()) {
                    currentProcess.setQuantum(0);
                    currentProcess.setTurnAroundTime(currentTime - currentProcess.getArrivalTime());
                    currentProcess.setWaitingTime(currentProcess.getTurnAroundTime() - currentProcess.getBurstTime());
                    finishedProcesses.add(currentProcess);
                    currentProcess.resetUsed();
                    currentProcess = null;
                }
                // --- SCENARIO I: Quantum Exhausted ---
                else if (consumed == q) {
                    currentProcess.setQuantum(q + 2);
                    readyQueue.add(currentProcess);
                    currentProcess.resetUsed();
                    currentProcess = null;
                }
                // --- SCENARIO III: SJF Phase (50% - 100%) ---
                else if (consumed >= q50) {
                    Process shortest = getShortestJob();
                    if (shortest != null && shortest.getRemainingTime() < currentProcess.getRemainingTime()) {
                        currentProcess.setQuantum(q + (q - consumed));
                        readyQueue.add(currentProcess);
                        currentProcess.resetUsed();
                        currentProcess = null;
                    }
                }
                // --- SCENARIO II: Priority Phase (25% - 50%) ---
                else if (consumed >= q25) {
                    Process bestPrio = getBestPriority();
                    if (bestPrio != null && bestPrio.getPriority() < currentProcess.getPriority()) {
                        currentProcess.setQuantum(q + (int)Math.ceil((q - consumed) / 2.0));
                        readyQueue.add(currentProcess);
                        currentProcess.resetUsed();
                        currentProcess = null;
                    }
                }
            } else {
                currentTime++;
            }
        }
        printResults();
    }

    private void checkArrivals(List<Process> pool, int time) {
        Iterator<Process> it = pool.iterator();
        while (it.hasNext()) {
            Process p = it.next();
            if (p.getArrivalTime() <= time) {
                readyQueue.add(p);
                it.remove();
            }
        }
    }

    private Process getBestPriority() {
        if (readyQueue.isEmpty()) return null;
        return readyQueue.stream().min(Comparator.comparingInt(Process::getPriority)).get();
    }

    private Process getShortestJob() {
        if (readyQueue.isEmpty()) return null;
        return readyQueue.stream().min(Comparator.comparingInt(Process::getRemainingTime)).get();
    }

    private void printResults() {
        System.out.println("Execution Order: " + executionOrder);
        double totalWait = 0, totalTurn = 0;
        finishedProcesses.sort(Comparator.comparing(Process::getName));
        for (Process p : finishedProcesses) {
            System.out.println(p.getName() + " -> Wait: " + p.getWaitingTime() +
                    ", Turnaround: " + p.getTurnAroundTime() +
                    ", Quantum History: " + p.getQuantumHistory());
            totalWait += p.getWaitingTime();
            totalTurn += p.getTurnAroundTime();
        }
        System.out.println("Average Waiting Time: " + (totalWait / finishedProcesses.size()));
        System.out.println("Average Turnaround Time: " + (totalTurn / finishedProcesses.size()));
    }
}