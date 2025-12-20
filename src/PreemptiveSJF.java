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
         printResults();
    }
        private void printResults() {
        // the scheduler already filled ExcutionOrder during scheduling
        System.out.println("\"executionOrder\": " + ExcutionOrder + ",");

        // processesWithData holds completed processes with waiting/turnaround set
        List<Process> sorted = new ArrayList<>(processesWithData);
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
