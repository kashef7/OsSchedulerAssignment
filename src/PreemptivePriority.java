import java.util.PriorityQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public class PreemptivePriority extends Scheduler {
    PriorityQueue<Process> readyQueue = new PriorityQueue<>((a, b) -> {
        if (a.getPriority() != b.getPriority()) {
            return Integer.compare(a.getPriority(), b.getPriority());
        }
        return Integer.compare(a.getArrivalTime(), b.getArrivalTime());
    });

    // aging interval (in time units). Default 5 to match your test JSON.
    private int agingInterval;

    // constructors
  
    public PreemptivePriority(int agingInterval) {
        this.agingInterval = agingInterval;
    }

    // setter (dynamic change at runtime)
    public void setAgingInterval(int agingInterval) {
        this.agingInterval = agingInterval;
    }

    // finished processes and stats
    List<Process> processesWithData = new ArrayList<>();

    void checkArrivals(int currentTime, List<Process> incomingProcesses) {
        List<Process> arrivedProcesses = new ArrayList<>();
        for (Process p : incomingProcesses) {
            if (p.getArrivalTime() <= currentTime) {
                readyQueue.add(p);
                arrivedProcesses.add(p);
            }
        }
        incomingProcesses.removeAll(arrivedProcesses);
    }

    @Override
    void schedule(int noOfProcesses, int roundRobinTimeQuantum, int ContextSwitching) {
        int totalTime = 0;
        int completed = 0;
        Process currentProcess = null;
        List<Process> incomingProcesses = new ArrayList<>(processes);

        // clear parent queue and list
        ExcutionOrder.clear();
        processesWithData.clear();

        while (completed < noOfProcesses) {
            // bring arrivals at current time (available before scheduling)
            checkArrivals(totalTime, incomingProcesses);

            // If nothing left anywhere, break to avoid infinite loop
            if (readyQueue.isEmpty() && currentProcess == null && incomingProcesses.isEmpty()) {
                break;
            }

            // Aging using configurable interval
            if (agingInterval > 0 && totalTime > 0 && totalTime % agingInterval == 0) {
                List<Process> tempList = new ArrayList<>();
                while (!readyQueue.isEmpty()) {
                    Process p = readyQueue.poll();
                    if (p.getPriority() > 0) {
                        p.setPriority(p.getPriority() - 1);
                    }
                    tempList.add(p);
                }
                readyQueue.addAll(tempList);
            }

            // Preemption check immediately after aging/arrivals
            if (currentProcess != null && !readyQueue.isEmpty()) {
                Process top = readyQueue.peek();
                if (top.getPriority() < currentProcess.getPriority()) {
                    // preempt current immediately (will cause context switch before next run)
                    readyQueue.add(currentProcess);
                    currentProcess = null;
                }
            }

            // If no process is running, pick the next one (after context switch)
            if (currentProcess == null && !readyQueue.isEmpty()) {
                if (totalTime > 0) {
                    for (int i = 0; i < ContextSwitching; i++) {
                        totalTime++;
                        checkArrivals(totalTime, incomingProcesses);

                        // apply aging during context switch if hits interval
                        if (agingInterval > 0 && totalTime > 0 && totalTime % agingInterval == 0) {
                            List<Process> tmp = new ArrayList<>();
                            while (!readyQueue.isEmpty()) {
                                Process p = readyQueue.poll();
                                if (p.getPriority() > 0) {
                                    p.setPriority(p.getPriority() - 1);
                                }
                                tmp.add(p);
                            }
                            readyQueue.addAll(tmp);
                        }
                    }
                }
                currentProcess = readyQueue.poll();

                // RECORD the start/resume of a process (one entry per selection)
                if (currentProcess != null) {
                    ExcutionOrder.add(currentProcess.getName());
                }
            }

            // Run the current process for one time unit
            if (currentProcess != null) {
                currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);
                totalTime++;

                // arrivals at end of tick
                checkArrivals(totalTime, incomingProcesses);

                // immediate preemption check for next unit
                if (!readyQueue.isEmpty() && readyQueue.peek().getPriority() < currentProcess.getPriority()) {
                    readyQueue.add(currentProcess);
                    currentProcess = null;
                    continue;
                }

                if (currentProcess != null && currentProcess.getRemainingTime() == 0) {
                    currentProcess.setTurnAroundTime(totalTime - currentProcess.getArrivalTime());
                    currentProcess.setWaitingTime(currentProcess.getTurnAroundTime() - currentProcess.getBurstTime());
                    processesWithData.add(currentProcess);
                    completed++;
                    currentProcess = null;
                }
            } else {
                // no process to run -> advance time to next arrival if any, otherwise by 1
                if (!incomingProcesses.isEmpty()) {
                    int nextArrival = incomingProcesses.stream().mapToInt(Process::getArrivalTime).min().orElse(totalTime + 1);
                    while (totalTime < nextArrival) {
                        totalTime++;
                        checkArrivals(totalTime, incomingProcesses);
                        if (agingInterval > 0 && totalTime > 0 && totalTime % agingInterval == 0) {
                            List<Process> tmp = new ArrayList<>();
                            while (!readyQueue.isEmpty()) {
                                Process p = readyQueue.poll();
                                if (p.getPriority() > 0) {
                                    p.setPriority(p.getPriority() - 1);
                                }
                                tmp.add(p);
                            }
                            readyQueue.addAll(tmp);
                        }
                    }
                } else {
                    totalTime++;
                }
            }
        }

        printStats();
    }

    void printStats() {
        System.out.println("Processes execution order:");
        for (String name : ExcutionOrder) {
            System.out.print(name + " ");
        }
        System.out.println();

        double totalWaitingTime = 0;
        double totalTurnaroundTime = 0;

        // Sort finished processes by name so output is stable and matches test order (P1..Pn)
        List<Process> sorted = new ArrayList<>(processesWithData);
        Collections.sort(sorted, Comparator.comparing(Process::getName));

        System.out.println("Process\tWaiting Time\tTurnaround Time");
        for (Process p : sorted) {
            System.out.println(p.getName() + "\t" + p.getWaitingTime() + "\t\t" + p.getTurnAroundTime());
            totalWaitingTime += p.getWaitingTime();
            totalTurnaroundTime += p.getTurnAroundTime();
        }

        int n = sorted.size();
        if (n > 0) {
            System.out.printf("Average Waiting Time: %.2f\n", totalWaitingTime / n);
            System.out.printf("Average Turnaround Time: %.2f\n", totalTurnaroundTime / n);
        } else {
            System.out.println("Average Waiting Time: 0.00");
            System.out.println("Average Turnaround Time: 0.00");
        }
    }
}