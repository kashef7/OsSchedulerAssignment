import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

public class PreemptiveSJF extends Scheduler {

    PriorityQueue<Process> readyQueue =  new PriorityQueue<>((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));

    @Override
    void schedule(int noOfProcesses, int roundRobinTimeQuantum, int ContextSwitching) {
        Scanner scanner = new Scanner(System.in);
        List<Process> finishedProcess = new ArrayList<>();
        String name;
        int arrivalTime;
        int burstTime;
        int totalTime = 0;
        for(int i = 0; i < noOfProcesses;i++){
            System.out.println("Enter Process:" +i+" name:");
            name = scanner.next();
            System.out.println("Enter Process:" +i+" Arrival Time:");
            arrivalTime = scanner.nextInt();
            System.out.println("Enter Process:" +i+" Burst Time:");
            burstTime = scanner.nextInt();
            Process process = new Process(arrivalTime,burstTime,name);
            process.setPriority(burstTime);
            processes.add(process);
        }
        while(finishedProcess.size() < noOfProcesses){
            continue;
        }
    }
}
