import java.lang.reflect.Executable;
import java.util.List;

public class Main{
    public static void main(String[] args) {
        PreemptiveSJF preemptiveSJF = new PreemptiveSJF();
        Process p1 = new Process(0,6,"p1");
        Process p2 = new Process(0,3,"p2");
        Process p3 = new Process(0,8,"p3");
        Process p4 = new Process(0,4,"p4");
        Process p5 = new Process(0,2,"p5");
        preemptiveSJF.processes = List.of(new Process[]{p1, p2, p3, p4, p5});
        preemptiveSJF.schedule(5,0,1);
        while(!preemptiveSJF.ExcutionOrder.isEmpty()){
            System.out.println(preemptiveSJF.ExcutionOrder.poll());
        }
        for(Process process: preemptiveSJF.processesWithData){
            System.out.print("name:"+process.getName() +","+"Waiting Time:"+process.getWaitingTime() + "," + "Turnaround Time:" + process.getTurnAroundTime() + "\n");
        }
    }
}