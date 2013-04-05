/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dataart.devicehive;

import java.util.Vector;

public class PostService implements Runnable {

    private DeviceData device;

    public PostService(DeviceData device) {
        this.device = device;
        updateCommandsToSend = new Vector();
        notificationsToSend = new Vector();
    }

    private class UpdateCommandStructure {

        long cmdId;
        String status;
        Object result;
    };

    private class NotificationStructure {

        String notification;
        Object params;
    };
    private boolean isRun = false;
    private Thread thread;
    private Vector updateCommandsToSend;
    private Vector notificationsToSend;

    public void updateCommand(long cmdId, String status, Object result) {
        if (isRun) {
            UpdateCommandStructure updateCommand = new UpdateCommandStructure();

            updateCommand.cmdId = cmdId;
            updateCommand.status = status;
            updateCommand.result = result;

            synchronized (this) {
                updateCommandsToSend.addElement(updateCommand);
            }
        }

    }

    public void insertNotification(String notification, Object params) {

        NotificationStructure notificationStructure = new NotificationStructure();

        notificationStructure.notification = notification;
        notificationStructure.params = params;

        synchronized (this) {
            notificationsToSend.addElement(notificationStructure);
        }
    }

    public void run() {
        while (isRun) {
            try {

                UpdateCommandStructure update = null;
                synchronized (this) {
                    if (!updateCommandsToSend.isEmpty()) {
                        update = (UpdateCommandStructure) updateCommandsToSend.firstElement();
                        updateCommandsToSend.removeElementAt(0);
                    }
                }

                if (update != null) {
                    RestClient.updateCommand(device, update.cmdId, update.status, update.result);
                }

                NotificationStructure notification = null;
                synchronized (notificationsToSend) {
                    if (!notificationsToSend.isEmpty()) {
                        notification = (NotificationStructure) notificationsToSend.firstElement();
                        notificationsToSend.removeElementAt(0);
                    }
                }

                if (notification != null) {
                    RestClient.insertNotification(device, notification.notification, notification.params);
                }

            } catch (Exception ex) {

                DebugUtils.diagnosticLog("PollService run cycle  error: " + ex.getMessage());
            }
        }
    }

    public void stop() {
        isRun = false;
        thread = null;

    }

    public void start() {
        isRun = true;


        thread = new Thread(this);
        thread.start();
    }
}
