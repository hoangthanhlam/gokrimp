/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gokrimp;

/**
 * event class
 * @author thoang
 */
class Event implements Comparable<Event> {
    int id; // id of the event
    int ts; // timestamp
    int gap; //gap to previous timestamp
    
    @Override
    /**
     * compare two events by timestamp
     */
    public int compareTo(Event o) {
        return this.ts - o.ts ;
    }
}

