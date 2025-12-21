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
    processes.sort(Comparator.comparingInt(Process::getArrivalTime));
    int arrivalIndex = 0;
    Process currentProcess = null;
    int timeInCurrentQuantum = 0;

    // Initial load for t=0
    arrivalIndex = updateArrivingProcesses(arrivalIndex);

    while (completedProcesses.size() < noOfProcesses) {
        if (currentProcess == null) {
            if (readyQueue.isEmpty()) {
                currentTime++;
                arrivalIndex = updateArrivingProcesses(arrivalIndex);
                continue;
            } else {
                currentProcess = readyQueue.remove(0);
                timeInCurrentQuantum = 0;
                runningProcessName = currentProcess.getName();
                segmentStartTime = currentTime;
                if (ExcutionOrder.isEmpty() || !((LinkedList<String>) ExcutionOrder).peekLast().equals(runningProcessName)) {
                    ExcutionOrder.add(runningProcessName);
                }
            }
        }

        // 1. Execute one time unit
        executeOneUnit(currentProcess);
        timeInCurrentQuantum++;

        // 2. CRITICAL: Handle Arrivals immediately after time increment 
        // but BEFORE any preemption re-queuing logic.
        arrivalIndex = updateArrivingProcesses(arrivalIndex);

        // 3. Check if finished
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

        // 4. AG Quantum logic (Preemption scenarios)
        int Q = currentProcess.getQuantum();
        int limit1 = (int) Math.ceil(Q * 0.25);
        int limit2 = limit1 + (int) Math.ceil(Q * 0.25);
        boolean preempted = false;

        // Scenario II: 25% Priority Preemption
        // Scenario II: 25% Priority Preemption
if (timeInCurrentQuantum == limit1) {
    Process best = getBestPriority();
    // Must be STRICTLY better priority
    if (best != null && best.getPriority() < currentProcess.getPriority()) {
        recordGanttForRunning();
        
        int unused = Q - timeInCurrentQuantum;
        currentProcess.updateQuantum(Q + (int) Math.ceil(unused / 2.0));
        
        // --- ORDER MATTERS HERE ---
        readyQueue.remove(best);          // 1. Take the new guy out of the queue
        currentProcess.resetUsed();
        readyQueue.add(currentProcess);   // 2. Put the old guy at the back
        currentProcess = best;            // 3. Switch
        
        startNewSegment(currentProcess);
        timeInCurrentQuantum = 0;
        preempted = true;
    }
}

// Scenario III: 50% SJF Preemption
if (!preempted && timeInCurrentQuantum >= limit2) {
    Process bestSJ = getShortestJob();
    // Must be STRICTLY shorter remaining time
    if (bestSJ != null && bestSJ.getRemainingTime() < currentProcess.getRemainingTime()) {
        recordGanttForRunning();
        
        int unused = Q - timeInCurrentQuantum;
        currentProcess.updateQuantum(Q + unused);

        // --- ORDER MATTERS HERE ---
        readyQueue.remove(bestSJ);        // 1. Take the new guy out
        currentProcess.resetUsed();
        readyQueue.add(currentProcess);   // 2. Put the old guy at the back
        currentProcess = bestSJ;          // 3. Switch

        startNewSegment(currentProcess);
        timeInCurrentQuantum = 0;
        preempted = true;
    }
}

        // Scenario I: Quantum Exhausted
        if (!preempted && timeInCurrentQuantum == Q) {
            recordGanttForRunning();
            currentProcess.updateQuantum(Q + 2);
            currentProcess.resetUsed();
            readyQueue.add(currentProcess);

            currentProcess = null;
            timeInCurrentQuantum = 0;
        }
    }
    printResults();
}

private void startNewSegment(Process p) {
    runningProcessName = p.getName();
    segmentStartTime = currentTime;
    if (ExcutionOrder.isEmpty() || !((LinkedList<String>) ExcutionOrder).peekLast().equals(runningProcessName)) {
        ExcutionOrder.add(runningProcessName);
    }
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
        Process best = null;
        for (Process p : readyQueue) {
            if (best == null || p.getRemainingTime() < best.getRemainingTime()) {
                best = p;
            }
        }
        return best;
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
     // set false after we finish debugging

}
