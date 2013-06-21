/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2013 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the  
 * GNU Affero General Public License for more details.  
 *  
 * You should have received a copy of the GNU Affero General Public License  
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.  
 */

package com.docdoku.core.workflow;

import com.docdoku.core.common.User;
import com.docdoku.core.util.Tools;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Date;

/**
 * <a href="Task.html">Task</a> is the smallest unit of work in a workflow.
 * 
 * @author Florent Garin
 * @version 1.0, 02/06/08
 * @since   V1.0
 */
@Table(name="TASK")
@javax.persistence.IdClass(com.docdoku.core.workflow.TaskKey.class)
@Entity
public class Task implements Serializable, Cloneable {

    
    @Id
    @ManyToOne(optional=false, fetch=FetchType.EAGER)
    @JoinColumns({
        @JoinColumn(name="ACTIVITY_STEP", referencedColumnName="STEP"),
        @JoinColumn(name="WORKFLOW_ID", referencedColumnName="WORKFLOW_ID")
    })
    private Activity activity;
    
    @Id
    private int num;
    
    private String title;
    
    @Lob
    private String instructions;

    private int duration=1;
    
    @javax.persistence.Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date startDate;
    
    
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumns({
        @JoinColumn(name="WORKER_LOGIN", referencedColumnName="LOGIN"),
        @JoinColumn(name="WORKER_WORKSPACE_ID", referencedColumnName="WORKSPACE_ID")
    })
    private User worker;
    
    private int targetIteration;
    private String closureComment;
    
    @javax.persistence.Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date closureDate;

    @Lob
    private String signature;
    
    private Status status=Status.NOT_STARTED;
    
    public enum Status {NOT_STARTED, IN_PROGRESS, APPROVED, REJECTED};
    
    
    public Task() {
    }

    public Task(int pNum, String pTitle, String pInstructions, User pWorker) {
        num=pNum;
        title=pTitle;
        worker=pWorker;
        instructions=pInstructions;
    }

    @XmlTransient
    public Activity getActivity() {
        return activity;
    }

    public int getWorkflowId() {
        return activity==null?0:activity.getWorkflowId();
    }

    public int getActivityStep() {
        return activity==null?0:activity.getStep();
    }
    
    
    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }


    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setStatus(Task.Status status) {
        this.status = status;
    }


    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getInstructions() {
        return instructions;
    }

    public User getWorker() {
        return worker;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDuration() {
        return duration;
    }

    public void setWorker(User worker) {
        this.worker = worker;
    }

    public void setClosureComment(String closureComment) {
        this.closureComment = closureComment;
    }

    public String getClosureComment() {
        return closureComment;
    }

    public int getTargetIteration() {
        return targetIteration;
    }

    public void setTargetIteration(int targetIteration) {
        this.targetIteration = targetIteration;
    }
    
    public TaskKey getKey() {
        return new TaskKey(new ActivityKey(getWorkflowId(), getActivityStep()),num);
    }
    
    public Date getClosureDate(){
        return closureDate;
    }

    public Task.Status getStatus() {
        return status;
    }

    public void setClosureDate(Date closureDate) {
        this.closureDate = closureDate;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void reject(String pComment, int pTargetIteration){
        reject(pComment, pTargetIteration, null);
    }

    public void reject(String pComment, int pTargetIteration, String pSignature){
        closureDate=new Date();
        closureComment=pComment;
        signature=pSignature;
        status=Status.REJECTED;
        targetIteration=pTargetIteration;
    }
    
    public void approve(String pComment, int pTargetIteration){
        approve(pComment, pTargetIteration, null);
    }

    public void approve(String pComment, int pTargetIteration, String pSignature){
        closureDate=new Date();
        closureComment=pComment;
        signature=pSignature;
        status=Status.APPROVED;
        targetIteration=pTargetIteration;
    }
    
    public void start(){
        if(isNotStarted()){
            startDate=new Date();
            status=Status.IN_PROGRESS;
        }
    }

    public void stop(){
        if(isInProgress()){
            status = Status.NOT_STARTED;
        }
    }
    
    public boolean isNotStarted(){
        //because of bug #6277781
        return status.ordinal()==Status.NOT_STARTED.ordinal();
    }
    
    public boolean isRejected(){
        //because of bug #6277781
        return status.ordinal()==Status.REJECTED.ordinal();
    }
    
    public boolean isApproved(){
        //because of bug #6277781
        return status.ordinal()==Status.APPROVED.ordinal();
    }
    
    public boolean isInProgress(){
        //because of bug #6277781
        return status.ordinal()==Status.IN_PROGRESS.ordinal();
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
	hash = 31 * hash + (activity==null?0:activity.hashCode());
        hash = 31 * hash + num;
	return hash;
    }
    
    @Override
    public boolean equals(Object pObj) {
        if (this == pObj) {
            return true;
        }
        if (!(pObj instanceof Task))
            return false;
        Task task = (Task) pObj;
        return ((Tools.safeEquals(task.activity,activity)) && (task.num==num));
    }
    
    @Override
    public String toString() {
        return title;
    }
    
    
    /**
     * perform a deep clone operation
     */
    @Override
    public Task clone() {
        Task clone = null;
        try {
            clone = (Task) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        if(startDate!=null)
            clone.startDate = (Date) startDate.clone();
        
        if(closureDate!=null)
            clone.closureDate = (Date) closureDate.clone();
        
        return clone;
    }
}
