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
        int inputOrder;

        ProcessState(Process p, int inputOrder) {
            this.p = p;
            this.remainingBurst = p.getBurstTime();
            this.currentPriority = p.getPriority();
            this.inputOrder = inputOrder;
        }
    }

    @Override
    void schedule(int noOfProcesses, int rrQuantum, int contextSwitch) {
        List<ProcessState> arrivalPool = new ArrayList<>();
        for (int i = 0; i < processes.size(); i++) {
            arrivalPool.add(new ProcessState(processes.get(i), i));
        }
        arrivalPool.sort(Comparator.comparingInt(a -> a.p.getArrivalTime()));

        PriorityQueue<ProcessState> readyQueue = new PriorityQueue<>((a, b) -> {
            if (a.currentPriority != b.currentPriority) 
                return Integer.compare(a.currentPriority, b.currentPriority);
            if (a.p.getArrivalTime() != b.p.getArrivalTime())
                return Integer.compare(a.p.getArrivalTime(), b.p.getArrivalTime());
            return Integer.compare(a.inputOrder, b.inputOrder);
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
                                   top.inputOrder < current.inputOrder) {
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
                        current.ageTimer = 0;
                        readyQueue.add(current);
                    }
                    
                    current = null;
                    if (completed == noOfProcesses) break;

                    ProcessState pending = readyQueue.poll();
                    pending.ageTimer = 0; 
                    
                    for (int i = 0; i < contextSwitch; i++) {
                        time++;
                        pending.ageTimer++;
                        if (agingInterval > 0 && pending.ageTimer >= agingInterval) {
                            pending.currentPriority = Math.max(1, pending.currentPriority - 1);
                            pending.ageTimer = 0;
                        }
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
                                             bestNow.inputOrder < pending.inputOrder);

                        if (betterPrio || betterArrival || betterOrig) {
                            execOrder.add(pending.p.getName());
                            timeline.add(pending.p.getName().toLowerCase() + "|" + time + "|");
                            readyQueue.add(pending);
                            pending = readyQueue.poll();
                            pending.ageTimer = 0;

                            for (int i = 0; i < contextSwitch; i++) {
                                time++;
                                pending.ageTimer++;
                                if (agingInterval > 0 && pending.ageTimer >= agingInterval) {
                                    pending.currentPriority = Math.max(1, pending.currentPriority - 1);
                                    pending.ageTimer = 0;
                                }
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
        if (agingInterval <= 0 || queue.isEmpty()) return;
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
        // Sort processes by numeric suffix if present, otherwise by name
        List<Process> sorted = new ArrayList<>(processes);
        sorted.sort((a, b) -> {
            String na = a.getName(), nb = b.getName();
            Integer ia = null, ib = null;
            try { ia = Integer.valueOf(na.replaceAll("\\D+", "")); } catch (Exception e) {}
            try { ib = Integer.valueOf(nb.replaceAll("\\D+", "")); } catch (Exception e) {}
            if (ia != null && ib != null) return ia.compareTo(ib);
            return na.compareTo(nb);
        });

        System.out.println("\"processResults\": [");
        double totalWait = 0;
        double totalTurn = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Process p = sorted.get(i);
            totalWait += p.getWaitingTime();
            totalTurn += p.getTurnAroundTime();
            System.out.print("  {\"name\": \"" + p.getName() + "\", \"waitingTime\": " + p.getWaitingTime()
                    + ", \"turnaroundTime\": " + p.getTurnAroundTime() + "}");
            if (i < sorted.size() - 1) System.out.println(",");
            else System.out.println();
        }
        System.out.println("],");
        double avgW = sorted.isEmpty() ? 0.0 : (totalWait / sorted.size());
        double avgT = sorted.isEmpty() ? 0.0 : (totalTurn / sorted.size());
        System.out.println("\"averageWaitingTime\": " + avgW + ",");
        System.out.println("\"averageTurnaroundTime\": " + avgT);
    }
}
