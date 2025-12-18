import java.util.*;

public class RoundRobin extends Scheduler {
    Queue<Process> processQueue = new LinkedList<>();

    @Override
    void schedule(int noOfProcesses, int roundRobinTimeQuantum, int ContextSwitching) {
        // Implementation of Round Robin scheduling algorithm
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));
        int currentTime = 0;
        int completedProcesses = 0;
        int index = 0;
        for (Process p : processes) {
            p.setPriority(p.getBurstTime());
        }

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
            if (currentProcess.getPriority() <= roundRobinTimeQuantum) {
                ExcutionOrder.add(currentProcess.getName() + "Starts at " + currentTime +
                        " and ends at " + (currentTime + currentProcess.getPriority()));
                currentTime += currentProcess.getPriority();
                completedProcesses++;
            } else {
                ExcutionOrder.add(currentProcess.getName() + "Starts at " + currentTime +
                        " and ends at " + (currentTime + roundRobinTimeQuantum));
                currentTime += roundRobinTimeQuantum;
                currentProcess.setPriority(currentProcess.getPriority() - roundRobinTimeQuantum);
                processQueue.add(currentProcess);
            }
            while (index < noOfProcesses && processes.get(index).getArrivalTime() <= currentTime) {
                processQueue.add(processes.get(index));
                index++;
            }
            if (!processQueue.isEmpty()) {
                currentTime += ContextSwitching;
            }
        }
    }
}
