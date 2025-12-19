import java.util.PriorityQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PreemptivePriority extends Scheduler {

    private PriorityQueue<Process> readyQueue;
    private int agingInterval;

    public PreemptivePriority(int agingInterval) {
        this.agingInterval = agingInterval;
        this.readyQueue = new PriorityQueue<>(
                (a, b) -> a.getPriority() != b.getPriority()
                        ? a.getPriority() - b.getPriority()
                        : a.getArrivalTime() - b.getArrivalTime()
        );
    }

    @Override
    void schedule(int noOfProcesses, int rrQuantum, int contextSwitch) {

        List<String> executionOrder = new ArrayList<>();
        List<Process> incoming = new ArrayList<>(processes);

        int time = 0;
        int completed = 0;
        int lastAgingTime = 0;

        Process current = null;

        while (completed < noOfProcesses) {

            for (int i = 0; i < incoming.size(); i++) {
                if (incoming.get(i).getArrivalTime() <= time) {
                    readyQueue.add(incoming.remove(i));
                    i--;
                }
            }

            if (time > 0 && (time - lastAgingTime) >= agingInterval) {
                List<Process> tmp = new ArrayList<>();
                while (!readyQueue.isEmpty()) {
                    Process p = readyQueue.poll();
                    if (p.getPriority() > 0)
                        p.setPriority(p.getPriority() - 1);
                    tmp.add(p);
                }
                readyQueue.addAll(tmp);
                lastAgingTime = time;
            }

            if (current != null && !readyQueue.isEmpty()) {
                if (readyQueue.peek().getPriority() < current.getPriority()) {
                    readyQueue.add(current);
                    current = null;

                    for (int i = 0; i < contextSwitch; i++) {
                        time++;
                        for (int j = 0; j < incoming.size(); j++) {
                            if (incoming.get(j).getArrivalTime() <= time) {
                                readyQueue.add(incoming.remove(j));
                                j--;
                            }
                        }
                    }
                }
            }

            if (current == null && !readyQueue.isEmpty()) {
                current = readyQueue.poll();
                if (executionOrder.isEmpty() ||
                        !executionOrder.get(executionOrder.size() - 1).equals(current.getName())) {
                    executionOrder.add(current.getName());
                }
            }

            if (current == null) {
                time++;
                continue;
            }

            current.setRemainingTime(current.getRemainingTime() - 1);
            time++;

            if (current.getRemainingTime() == 0) {
                current.setTurnAroundTime(time - current.getArrivalTime());
                current.setWaitingTime(
                        current.getTurnAroundTime() - current.getBurstTime()
                );
                completed++;
                current = null;

                for (int i = 0; i < contextSwitch; i++) {
                    time++;
                    for (int j = 0; j < incoming.size(); j++) {
                        if (incoming.get(j).getArrivalTime() <= time) {
                            readyQueue.add(incoming.remove(j));
                            j--;
                        }
                    }
                }
            }
        }

        System.out.println("Execution Order:");
        for (String s : executionOrder)
            System.out.print(s + " ");
        System.out.println();

        Collections.sort(processes, Comparator.comparing(Process::getName));

        double totalWT = 0;
        double totalTT = 0;

        System.out.println("Process   Waiting Time   Turnaround Time");
        for (Process p : processes) {
            totalWT += p.getWaitingTime();
            totalTT += p.getTurnAroundTime();
            System.out.printf(
                    "%-8s %-14d %-15d%n",
                    p.getName(),
                    p.getWaitingTime(),
                    p.getTurnAroundTime()
            );
        }

        System.out.printf("Average Waiting Time: %.1f%n", totalWT / noOfProcesses);
        System.out.printf("Average Turnaround Time: %.1f%n", totalTT / noOfProcesses);
    }
}