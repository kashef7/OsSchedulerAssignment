import java.util.*;

public class PreemptiveSJF extends Scheduler {

    PriorityQueue<Process> readyQueue = new PriorityQueue<>((a, b) -> {
        if (a.getRemainingTime() != b.getRemainingTime()) {
            return Integer.compare(a.getRemainingTime(), b.getRemainingTime());
        }
        return Integer.compare(a.getArrivalTime(), b.getArrivalTime());
    });

    List<Process> processesWithData = new ArrayList<>();

    @Override
    void schedule(int noOfProcesses, int roundRobinTimeQuantum, int ContextSwitching) {
        int totalTime = 0;
        int completed = 0;
        Process currentProcess = null;
        List<Process> incomingProcesses = new ArrayList<>(processes);
        processesWithData.clear();
        ExcutionOrder.clear();

        while (completed < noOfProcesses) {
            checkArrivals(incomingProcesses, totalTime);
            if (currentProcess != null && !readyQueue.isEmpty()) {
                if (readyQueue.peek().getRemainingTime() < currentProcess.getRemainingTime()) {
                    readyQueue.add(currentProcess);
                    currentProcess = null;
                }
            }

            if (currentProcess == null && !readyQueue.isEmpty()) {
                Process nextProcess = readyQueue.poll();

                if (totalTime > 0) {
                    for (int i = 0; i < ContextSwitching; i++) {
                        totalTime++;
                        checkArrivals(incomingProcesses, totalTime);
                    }
                }
                currentProcess = nextProcess;
                ExcutionOrder.add(currentProcess.getName());
            }
            if (currentProcess != null) {
                currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);
                totalTime++;
                if (currentProcess.getRemainingTime() == 0) {
                    currentProcess.setTurnAroundTime(totalTime - currentProcess.getArrivalTime());
                    currentProcess.setWaitingTime(currentProcess.getTurnAroundTime() - currentProcess.getBurstTime());
                    processesWithData.add(currentProcess);
                    completed++;
                    currentProcess = null;
                }
            } else {
                totalTime++;
            }
        }
    }

    private void checkArrivals(List<Process> incoming, int time) {
        Iterator<Process> iterator = incoming.iterator();
        while (iterator.hasNext()) {
            Process p = iterator.next();
            if (p.getArrivalTime() <= time) {
                readyQueue.add(p);
                iterator.remove();
            }
        }
    }
}
