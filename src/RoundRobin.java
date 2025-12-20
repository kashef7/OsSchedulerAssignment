import java.util.*;

public class RoundRobin extends Scheduler {
    Queue<Process> processQueue = new LinkedList<>();
    List<Process> outputProcesses = new LinkedList<>();
    Queue<Process> orderExecution = new LinkedList<>();

    @Override
    void schedule(int noOfProcesses, int roundRobinTimeQuantum, int ContextSwitching) {
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));
        int currentTime = 0;
        int completedProcesses = 0;
        int index = 0;
        for (Process p : processes) {
            p.setRemainingTime(p.getBurstTime());
            outputProcesses.add(new Process(p.getArrivalTime(), p.getBurstTime(), p.getName()));
            outputProcesses.get(outputProcesses.size() - 1).setRemainingTime(p.getRemainingTime());
        }
        Process lastProcess = null;
        while (completedProcesses < noOfProcesses) {

            while (index < noOfProcesses && processes.get(index).getArrivalTime() <= currentTime) {
                processQueue.add(processes.get(index));
                index++;
            }
            if (processQueue.isEmpty()) {
                currentTime++;
                continue;
            }

            Process currentProcess = processQueue.poll();
            if (lastProcess != null && lastProcess != currentProcess) {
                currentTime += ContextSwitching;
            }

            int j = 0;
            for (int i = 0; i < outputProcesses.size(); i++) {
                if (outputProcesses.get(i).getName().equals(currentProcess.getName())) {
                    j = i;
                    break;
                }
            }

            orderExecution.add(currentProcess);

            if (currentProcess.getRemainingTime() <= roundRobinTimeQuantum) {
                currentTime += currentProcess.getRemainingTime();
                outputProcesses.get(j).setTurnAroundTime(currentTime - currentProcess.getArrivalTime());
                outputProcesses.get(j).setWaitingTime(
                        outputProcesses.get(j).getTurnAroundTime() - currentProcess.getBurstTime());
                completedProcesses++;
                currentProcess.setRemainingTime(0);
                outputProcesses.get(j).setRemainingTime(0);
            } else {
                currentTime += roundRobinTimeQuantum;
                currentProcess.setRemainingTime(currentProcess.getRemainingTime() - roundRobinTimeQuantum);
            }
            while (index < noOfProcesses && processes.get(index).getArrivalTime() <= currentTime) {
                processQueue.add(processes.get(index));
                index++;
            }
            if (currentProcess.getRemainingTime() > 0) {
                processQueue.add(currentProcess);
            }

            lastProcess = currentProcess;
        }
        printResults();
    }

    void calculateAvgOfWaitingAndTurnAroundTime() {
        double totalWaitingTime = 0;
        double totalTurnAroundTime = 0;
        for (Process p : outputProcesses) {
            totalWaitingTime += p.getWaitingTime();
            totalTurnAroundTime += p.getTurnAroundTime();
        }
        System.out.println("Average Waiting Time: " + (totalWaitingTime / outputProcesses.size()));
        System.out.println("Average Turn Around Time: " + (totalTurnAroundTime / outputProcesses.size()));
    }

    void outputProcessesInfo() {
        for (Process p : outputProcesses) {
            System.out.println("\"name\": " + p.getName() + ", " +
                    "\"waitingTime\": " + p.getWaitingTime() + ", " +
                    "\"turnAroundTime\": " + p.getTurnAroundTime());
        }
    }

    void outputOrderExecution() {
        System.out.print("Order of Execution: ");
        for (Process p : orderExecution) {
            System.out.print(p.getName() + " ");
        }
        System.out.println();
    }
      void printResults() {
        // populate inherited ExcutionOrder from the internal orderExecution queue
        ExcutionOrder.clear();
        for (Process p : orderExecution) {
            ExcutionOrder.add(p.getName());
        }

        // build sorted copy of outputProcesses (by numeric suffix if present)
        List<Process> sorted = new ArrayList<>(outputProcesses);
        sorted.sort((a, b) -> {
            String na = a.getName();
            String nb = b.getName();
            Integer ia = null, ib = null;
            try { ia = Integer.valueOf(na.replaceAll("\\D+", "")); } catch (Exception e) {}
            try { ib = Integer.valueOf(nb.replaceAll("\\D+", "")); } catch (Exception e) {}
            if (ia != null && ib != null) return ia.compareTo(ib);
            return na.compareTo(nb);
        });

        // Copy results back into original 'processes' so Main can read actual values
        Map<String, Process> origByName = new HashMap<>();
        for (Process p : processes) origByName.put(p.getName(), p);
        for (Process p : sorted) {
            Process orig = origByName.get(p.getName());
            if (orig != null) {
                orig.setWaitingTime(p.getWaitingTime());
                orig.setTurnAroundTime(p.getTurnAroundTime());
                orig.setRemainingTime(p.getRemainingTime());
            }
        }

        // print header similar to AgScheduler
        System.out.println("Gantt Chart: "); // RoundRobin doesn't track segments; left blank
        System.out.println();
        System.out.println("\"executionOrder\": " + ExcutionOrder + ",");

        System.out.println("\"processResults\": [");
        double totalWait = 0;
        double totalTurn = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Process p = sorted.get(i);
            totalWait += p.getWaitingTime();
            totalTurn += p.getTurnAroundTime();
            System.out.print("  {\"name\": \"" + p.getName() + "\", \"waitingTime\": " + p.getWaitingTime() +
                             ", \"turnaroundTime\": " + p.getTurnAroundTime() + "}");
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
