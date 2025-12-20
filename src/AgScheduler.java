import java.util.*;
import static java.lang.Math.*;

public class AgScheduler extends Scheduler {
    private List<Process> readyQueue = new ArrayList<>();
    private List<Process> completedProcesses = new ArrayList<>();
    private List<String> ganttChart = new ArrayList<>();
    private int currentTime = 0;
    private int segmentStartTime = 0;
    private String runningProcessName = "";

        @Override
    void schedule(int noOfProcesses, int roundRobinTimeQuantum, int ContextSwitching) {
        // Ensure processes are sorted by arrival time
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));
        int arrivalIndex = 0;
        Process currentProcess = null;
        int timeInCurrentQuantum = 0;

        // Initial load for t=0
        arrivalIndex = updateArrivingProcesses(arrivalIndex);

        while (completedProcesses.size() < noOfProcesses) {
            // If no current process, pick from head of readyQueue or advance time if empty
            if (currentProcess == null) {
                if (readyQueue.isEmpty()) {
                    currentTime++;
                    arrivalIndex = updateArrivingProcesses(arrivalIndex);
                    continue;
                } else {
                    currentProcess = readyQueue.remove(0);
                    timeInCurrentQuantum = 0;
                    // start new segment
                    runningProcessName = currentProcess.getName();
                    segmentStartTime = currentTime;
                    if (ExcutionOrder.isEmpty() || !((LinkedList<String>) ExcutionOrder).peekLast().equals(runningProcessName)) {
                        ExcutionOrder.add(runningProcessName);
                    }
                }
            }

            // Execute one time unit
            executeOneUnit(currentProcess);
            // increment counters
            timeInCurrentQuantum++;
            arrivalIndex = updateArrivingProcesses(arrivalIndex);

            // If process finished, finalize and continue
            if (currentProcess.isFinished()) {
                recordGanttForRunning();
                currentProcess.updateQuantum(0);
                currentProcess.setTurnAroundTime(currentTime - currentProcess.getArrivalTime());
                currentProcess.setWaitingTime(currentProcess.getTurnAroundTime() - currentProcess.getBurstTime());
                completedProcesses.add(currentProcess);
                currentProcess = null;
                timeInCurrentQuantum = 0;
                continue;
            }

            // AG Quantum logic
            int Q = currentProcess.getQuantum();
            int limit1 = (int) Math.ceil(Q * 0.25);              // 25% threshold (priority check)
            int limit2 = limit1 + (int) Math.ceil(Q * 0.25);    // cumulative after next 25% (as in reference)
            boolean preempted = false;

            // Scenario II: after reaching limit1 (priority-based)
            if (timeInCurrentQuantum == limit1) {
                Process best = getBestPriority();
                // find strictly better priority in readyQueue
                if (best != null && best.getPriority() < currentProcess.getPriority()) {
                    // close current segment
                    recordGanttForRunning();

                    int unused = Q - timeInCurrentQuantum;
                    int newQ = Q + (int) Math.ceil(unused / 2.0); // Mean rule
                    currentProcess.updateQuantum(newQ);

                    // requeue current at tail
                    currentProcess.resetUsed();
                    readyQueue.add(currentProcess);

                    // remove chosen best from readyQueue and switch to it
                    readyQueue.remove(best);
                    currentProcess = best;
                    // start new segment for new current process
                    runningProcessName = currentProcess.getName();
                    segmentStartTime = currentTime;
                    if (ExcutionOrder.isEmpty() || !((LinkedList<String>) ExcutionOrder).peekLast().equals(runningProcessName)) {
                        ExcutionOrder.add(runningProcessName);
                    }
                    timeInCurrentQuantum = 0;
                    preempted = true;
                }
            }

            // Scenario III: after reaching or passing limit2 (SJF preemption)
            if (!preempted && timeInCurrentQuantum >= limit2) {
                Process bestSJ = getShortestJob();
                if (bestSJ != null && bestSJ.getRemainingTime() < currentProcess.getRemainingTime()) {
                    // close current segment
                    recordGanttForRunning();

                    int unused = Q - timeInCurrentQuantum;
                    int newQ = Q + unused; // Sum rule
                    currentProcess.updateQuantum(newQ);

                    // requeue current at tail
                    currentProcess.resetUsed();
                    readyQueue.add(currentProcess);

                    // switch to shortest job immediately
                    readyQueue.remove(bestSJ);
                    currentProcess = bestSJ;
                    runningProcessName = currentProcess.getName();
                    segmentStartTime = currentTime;
                    if (ExcutionOrder.isEmpty() || !((LinkedList<String>) ExcutionOrder).peekLast().equals(runningProcessName)) {
                        ExcutionOrder.add(runningProcessName);
                    }
                    timeInCurrentQuantum = 0;
                    preempted = true;
                }
            }

            // Scenario I: quantum exhausted exactly
            if (!preempted && timeInCurrentQuantum == Q) {
                // close current segment
                recordGanttForRunning();

                int newQ = Q + 2; // increase rule
                currentProcess.updateQuantum(newQ);
                currentProcess.resetUsed();
                readyQueue.add(currentProcess);

                currentProcess = null;
                timeInCurrentQuantum = 0;
            }
        }

        // print at end (existing method)
        printResults();
    }
    // execute one time unit for process p (does NOT add to execution order repeatedly)
    private void executeOneUnit(Process p) {
        p.reduceRemaining();
        p.incrementUsed();
        currentTime++;
    }

    // record the currently running process's segment into ganttChart (if any duration)
    private void recordGanttForRunning() {
        if (!runningProcessName.isEmpty() && currentTime > segmentStartTime) {
            ganttChart.add(runningProcessName + "|" + segmentStartTime + "-" + currentTime);
        }
    }

    private void handlePreemption(Process p) {
        p.resetUsed();
        readyQueue.add(p);
    }

    private int updateArrivingProcesses(int index) {
        while (index < processes.size() && processes.get(index).getArrivalTime() <= currentTime) {
            Process arriving = processes.get(index);
            if (!readyQueue.contains(arriving)) {
                readyQueue.add(arriving);
            }
            index++;
        }
        return index;
    }

    private Process getBestPriority() {
        return readyQueue.stream().min(Comparator.comparingInt(Process::getPriority)).orElse(null);
    }

    private Process getShortestJob() {
        return readyQueue.stream()
            .min(Comparator.comparingInt(Process::getRemainingTime)
                .thenComparingInt(Process::getArrivalTime))
            .orElse(null);
    }
    private void printResults() {
       System.out.println("Gantt Chart: " + String.join(" | ", ganttChart));
    System.out.println("\n\"executionOrder\": " + ExcutionOrder + ",");
    System.out.println("\"processResults\": [");

    // Make a sorted copy of completedProcesses by process name (P1, P2, P3...)
    List<Process> sorted = new ArrayList<>(completedProcesses);
    sorted.sort((a, b) -> {
        String na = a.getName();
        String nb = b.getName();
        // try to extract numeric suffix for numeric ordering
        Integer ia = null, ib = null;
        try {
            ia = Integer.valueOf(na.replaceAll("\\D+", ""));
        } catch (Exception e) { /* ignore */ }
        try {
            ib = Integer.valueOf(nb.replaceAll("\\D+", ""));
        } catch (Exception e) { /* ignore */ }
        if (ia != null && ib != null) return ia.compareTo(ib);
        return na.compareTo(nb);
    });

    double totalWait = 0;
    double totalTurn = 0;

    for (int i = 0; i < sorted.size(); i++) {
        Process p = sorted.get(i);
        totalWait += p.getWaitingTime();
        totalTurn += p.getTurnAroundTime();

        System.out.print("  {\"name\": \"" + p.getName() + "\", \"waitingTime\": " + p.getWaitingTime() +
                         ", \"turnaroundTime\": " + p.getTurnAroundTime() +
                         ", \"quantumHistory\": " + p.getQuantumHistory() + "}");
        if (i < sorted.size() - 1) System.out.println(",");
        else System.out.println("");
    }

    System.out.println("],");
    System.out.println("\"averageWaitingTime\": " + (totalWait / completedProcesses.size()) + ",");
    System.out.println("\"averageTurnaroundTime\": " + (totalTurn / completedProcesses.size()));
    }

}
