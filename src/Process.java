public class Process {
    private int arrivalTime;
    private int burstTime;
    private int priority;
    private int remainingTime;
    private int waitingTime;
    private int turnAroundTime;
    private String name;

    public Process(int arriveTime, int burstTime, String name) {
        this.arrivalTime = arriveTime;
        this.burstTime = burstTime;
        this.name = name;
        waitingTime = 0;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public void setBurstTime(int burstTime) {
        this.burstTime = burstTime;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public void setTurnAroundTime(int turnAroundTime) {
        this.turnAroundTime = turnAroundTime;
    }

    public int getTurnAroundTime() {
        return turnAroundTime;
    }

    public String getName() {

        return name;
    }
}