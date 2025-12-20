import java.util.*;

public class PreemptivePriority extends Scheduler {
    private final int agingInterval;

    public PreemptivePriority(int agingInterval) {
        this.agingInterval = agingInterval;
    }

    private static class ProcessState {
        Process p;
        int remainingBurst;
        int currentPriority;
        int ageTimer = 0;

        ProcessState(Process p) {
            this.p = p;
            this.remainingBurst = p.getBurstTime();
            this.currentPriority = p.getPriority();
        }
    }

    @Override
    void schedule(int noOfProcesses, int rrQuantum, int contextSwitch) {
        List<ProcessState> arrivalPool = new ArrayList<>();
        for (Process p : processes) arrivalPool.add(new ProcessState(p));
        arrivalPool.sort(Comparator.comparingInt(a -> a.p.getArrivalTime()));

        PriorityQueue<ProcessState> readyQueue = new PriorityQueue<>((a, b) -> {
            if (a.currentPriority != b.currentPriority) 
                return Integer.compare(a.currentPriority, b.currentPriority);
            if (a.p.getArrivalTime() != b.p.getArrivalTime())
                return Integer.compare(a.p.getArrivalTime(), b.p.getArrivalTime());
            return Integer.compare(a.p.getPriority(), b.p.getPriority());
        });

        List<String> execOrder = new ArrayList<>();
        List<String> timeline = new ArrayList<>();
        int time = 0, completed = 0, startSeg = 0;
        ProcessState current = null;

        while (completed < noOfProcesses) {
            while (!arrivalPool.isEmpty() && arrivalPool.get(0).p.getArrivalTime() <= time) {
                readyQueue.add(arrivalPool.remove(0));
            }

            if (current != null) {
                ProcessState top = readyQueue.peek();
                boolean finished = current.remainingBurst <= 0;
                boolean preempted = false;

                if (top != null && !finished) {
                    if (top.currentPriority < current.currentPriority) {
                        preempted = true;
                    } else if (top.currentPriority == current.currentPriority) {
                        if (top.p.getArrivalTime() < current.p.getArrivalTime()) {
                            preempted = true;
                        } else if (top.p.getArrivalTime() == current.p.getArrivalTime() && 
                                   top.p.getPriority() < current.p.getPriority()) {
                            preempted = true;
                        }
                    }
                }

                if (finished || preempted) {
                    timeline.add(current.p.getName().toLowerCase() + "|" + startSeg + "->" + time + "|");
                    
                    if (finished) {
                        current.p.setTurnAroundTime(time - current.p.getArrivalTime());
                        current.p.setWaitingTime(current.p.getTurnAroundTime() - current.p.getBurstTime());
                        completed++;
                    } else {
                        readyQueue.add(current);
                    }
                    
                    current = null;
                    if (completed == noOfProcesses) break;

                    ProcessState pending = readyQueue.poll();
                    pending.ageTimer = 0; 
                    
                    for (int i = 0; i < contextSwitch; i++) {
                        time++;
                        applyAging(readyQueue);
                        while (!arrivalPool.isEmpty() && arrivalPool.get(0).p.getArrivalTime() <= time) {
                            readyQueue.add(arrivalPool.remove(0));
                        }
                    }

                    ProcessState bestNow = readyQueue.peek();
                    if (bestNow != null) {
                        boolean betterPrio = bestNow.currentPriority < pending.currentPriority;
                        boolean betterArrival = (bestNow.currentPriority == pending.currentPriority && 
                                                bestNow.p.getArrivalTime() < pending.p.getArrivalTime());
                        boolean betterOrig = (bestNow.currentPriority == pending.currentPriority && 
                                             bestNow.p.getArrivalTime() == pending.p.getArrivalTime() && 
                                             bestNow.p.getPriority() < pending.p.getPriority());

                        if (betterPrio || betterArrival || betterOrig) {
                            execOrder.add(pending.p.getName());
                            timeline.add(pending.p.getName().toLowerCase() + "|" + time + "|");
                            readyQueue.add(pending);
                            pending = readyQueue.poll();
                            pending.ageTimer = 0;

                            for (int i = 0; i < contextSwitch; i++) {
                                time++;
                                applyAging(readyQueue);
                            }
                        }
                    }

                    current = pending;
                    execOrder.add(current.p.getName());
                    startSeg = time;
                }
            } else if (!readyQueue.isEmpty()) {
                current = readyQueue.poll();
                current.ageTimer = 0;
                execOrder.add(current.p.getName());
                startSeg = time;
            } else {
                time++;
                continue;
            }

            if (current != null) {
                current.remainingBurst--;
                time++;
                applyAging(readyQueue);
            }
        }

        System.out.println("executionOrder: " + execOrder);
        System.out.println("output: " + String.join("", timeline));
        printFinalStats();
    }

    private void applyAging(PriorityQueue<ProcessState> queue) {
        if (queue.isEmpty()) return;
        List<ProcessState> temp = new ArrayList<>();
        while (!queue.isEmpty()) {
            ProcessState ps = queue.poll();
            ps.ageTimer++;
            if (ps.ageTimer >= agingInterval) {
                ps.currentPriority = Math.max(1, ps.currentPriority - 1);
                ps.ageTimer = 0;
            }
            temp.add(ps);
        }
        queue.addAll(temp);
    }

    private void printFinalStats() {
        processes.sort(Comparator.comparing(Process::getName));
        double twt = 0, ttt = 0;
        for (Process p : processes) {
            System.out.printf("{\"name\": \"%s\", \"waitingTime\": %d, \"turnaroundTime\": %d},\n", 
                p.getName(), p.getWaitingTime(), p.getTurnAroundTime());
            twt += p.getWaitingTime();
            ttt += p.getTurnAroundTime();
        }
        System.out.printf("Average Waiting Time: %.1f\n", (twt / processes.size()));
        System.out.printf("Average Turnaround Time: %.1f\n", (ttt / processes.size()));
    }
}
