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
}