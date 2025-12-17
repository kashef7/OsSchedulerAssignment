import java.util.List;

abstract class Scheduler {
    List<Process> processes;
    abstract void schedule(int noOfProcesses, int roundRobinTimeQuantum, int ContextSwitching);

}
