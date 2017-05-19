package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.telephony.TelephonyManager;
import android.content.Context;
import java.net.ServerSocket;
import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */


class Message implements Comparable<Message>{

    String data;
    int proposedSeq;
    int processId;
    String tag;

    Message(String data, int proposedSeq, int processId, String tag){
        this.data = data;
        this.proposedSeq = proposedSeq;
        this.processId = processId;
        this.tag = tag;
    }

    public int getProposedSeq() {return proposedSeq;}
    public int getProcessId() {return processId;}

    @Override
    public int compareTo(Message another) {
        if(this.proposedSeq<another.proposedSeq)
            return  -1;
        else if(this.proposedSeq>another.proposedSeq)
            return 1;
        return 0;
    }
}


public class GroupMessengerActivity extends Activity implements View.OnClickListener {

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    static final int SERVER_PORT = 10000;
    static int s = -1;
    static int tempCount = 0;
    static int count = 0;
    static int count1 = 0;
    static int processid = 0;
    static String broadcast = null;  //** Very Important variable **
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static int CTCount = 0;
    static boolean failedNode = false;

    static LinkedList<String> REMOTE_PORTS = new LinkedList<String>();
    static LinkedList<Message> tempList = new LinkedList<Message>();
    static LinkedList<String>  list1 = new LinkedList();
    static LinkedList<Integer> list2 = new LinkedList();
    static LinkedList<Integer> processList = new LinkedList();
    static ArrayList<Message> queueList = new ArrayList<Message>();
    static LinkedList<Message> tempList1 = new LinkedList<Message>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        REMOTE_PORTS.add(REMOTE_PORT0);
        REMOTE_PORTS.add(REMOTE_PORT1);
        REMOTE_PORTS.add(REMOTE_PORT2);
        REMOTE_PORTS.add(REMOTE_PORT3);
        REMOTE_PORTS.add(REMOTE_PORT4);

        processList.add(0);
        processList.add(0);
        processList.add(0);
        processList.add(0);
        processList.add(0);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.e(TAG,"Server socket created");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;

        }

        final Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        EditText inputText = (EditText) findViewById(R.id.editText1);
                        String input = String.valueOf(inputText.getText());  //input contains the message received from editText1
                        inputText.setText("");
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, input, myPort);

                    }
                }

        );



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    @Override
    public void onClick(View v) {

    }

//-----------------------------------------------------------------------------------------------------------------------------------------------------

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.e(TAG,"Server Task");

            for(int i=0;i<9999;i++){       //This loop ensures that messages can be sent 9999 times on both sides
                try {
                    Socket s1 = serverSocket.accept();
                    Log.e(TAG,"Listening...");
                    DataOutputStream dOS = new DataOutputStream(s1.getOutputStream());
                    DataInputStream dIS = new DataInputStream(s1.getInputStream());
                    String message = dIS.readUTF();
                    String[] array = message.split(":");

                    if(array.length==2){ //Final notification
                        Log.i(TAG,"FAILURE PART");
                        failedNode = true;
                        dOS.writeUTF("Ack");
                    }

                    if(array.length==3){
                        Log.i(TAG,"ST Received: "+message);
                        message = array[0]; //Original message
                        processid = Integer.parseInt(array[1]);  //This is the the avd which sent the message
                        int ownId = Integer.parseInt(array[2]);  //Id of the avd on which the message has been received
                        String message1 = processid+":"+(++s)+":"+ownId;
                        Log.i(TAG,"proposal to _ from _ "+message1);
                        dOS.writeUTF(message1);
                        Message mObject = new Message(message, s, processid, "0");   //Created an object of class message
                        queueList.add(mObject); //Inserted the obtained message in a list too

                    }else if(array.length==6){  //Need to use this piece of code only after final broadcast
                        processid = Integer.parseInt(array[5]);  //This is the the avd which sent the message
                        count1++;
                        Log.e(TAG,"Count "+count1);
                        Log.i(TAG,message);
                        dOS.writeUTF("Ack");
                        String finalMessage = array[0];
                        String maxSeq = array[1];
                        if(s < Integer.parseInt(maxSeq))
                            s = Integer.parseInt(maxSeq);
                        int processId = Integer.parseInt(array[3]); //avd of the process which sent the max proposal
                        Log.i(TAG,"LIST");

                        for(int x=0;x<queueList.size();x++){
                            Log.i(TAG,queueList.get(x).data+":"+queueList.get(x).proposedSeq+":"+queueList.get(x).tag
                                    +":"+queueList.get(x).processId);
                        }

                        for(int x=0;x<queueList.size();x++){    // Replace seq of message in queue with maxSeq
                            if(queueList.get(x).data.equals(finalMessage)){
                                queueList.get(x).proposedSeq=Integer.parseInt(maxSeq);
                                queueList.get(x).tag="1";
                                queueList.get(x).processId = processId;
                                Log.i(TAG,"Element found and max Seq updated");
                            }
                        }

                        Collections.sort(queueList, new Comparator<Message>() {  //Sort by priority of proposedSeq and if proposedSeq is same then processId
                            @Override
                            public int compare(Message lhs, Message rhs) {
                                if (lhs.getProposedSeq() < rhs.getProposedSeq())
                                    return -1;
                                else if (lhs.getProposedSeq() > rhs.getProposedSeq())
                                    return 1;
                                else
                                    return SecondSort(lhs, rhs);
                            }
                            public int SecondSort(Message lhs,Message rhs) {
                                if(lhs.getProcessId()<rhs.getProcessId())
                                    return -1;
                                else if(lhs.getProcessId()>rhs.getProcessId())
                                    return 1;
                                else
                                    return 0;
                            }
                        });

                        Collections.sort(queueList);

                        Log.i(TAG,"LIST AFTER UPDATE");
                        for(int x=0;x<queueList.size();x++){
                            Log.i(TAG,queueList.get(x).data+":"+queueList.get(x).proposedSeq+":"+queueList.get(x).tag
                                    +":"+queueList.get(x).processId);
                        }

                        if(processid==11108)  //List which keeps track of number of final broadcasts received from each avd
                            processList.set(0, processList.get(0)+1);
                        else if(processid==11112)
                            processList.set(1, processList.get(1)+1);
                        else if(processid==11116)
                            processList.set(2, processList.get(2)+1);
                        else if(processid==11120)
                            processList.set(3, processList.get(3)+1);
                        else if(processid==11124)
                            processList.set(4, processList.get(4)+1);

                        for(int x=0;x<processList.size();x++){
                            Log.i(TAG,"Process List: "+processList.get(x));
                        }

                        int counter1 = 0;
                        for(int x=0;x<processList.size()-1;x++){
                            if(processList.get(x)==processList.get(x+1)){
                                counter1++;
                            }
                        }

                        if(REMOTE_PORTS.size()==5 && counter1==processList.size()-1){ //Delivers messages once it gets final broadcasts from all 5 avds
                            A: while(queueList.get(0).tag.equals("1")){
                                int headSeq = queueList.get(0).getProposedSeq();
                                for(int x=0;x<queueList.size();x++){
                                    if(queueList.get(x).proposedSeq==headSeq){
                                        tempList.add(queueList.get(x));
                                    }
                                }

                                for(int x=0;x<tempList.size();x++){
                                    if(tempList.get(x).tag.equals("1")){
                                        tempCount++;
                                    }
                                }

                                if(tempCount==tempList.size()){
                                    for(int x=0;x<tempList.size();x++){
                                        ContentValues keyValueToInsert = new ContentValues();
                                        keyValueToInsert.put("key", Integer.toString(count));
                                        keyValueToInsert.put("value", queueList.get(0).data);
                                        getContentResolver().insert(providerUri, keyValueToInsert);
                                        publishProgress(queueList.get(0).data + ":" + count);
                                        count++;
                                        queueList.remove(0);
                                        tempCount=0;
                                        tempList.clear();
                                    }
                                }else{
                                    tempCount=0;
                                    tempList.clear();
                                    break A;
                                }

                                if(queueList.isEmpty())
                                    break A;
                            }
                        }

                        int counter2 = 0;
                        for(int x=0;x<processList.size();x++){
                            if(processList.get(x)==5){
                                counter2++;
                            }
                        }

                        Log.i(TAG,"MAIN "+Integer.toString(REMOTE_PORTS.size())+"  "+Integer.toString(counter2)+" "+failedNode);

                        if((REMOTE_PORTS.size()==4 && counter2==4)){  //Part for failure handling
                            Log.i(TAG,"Main Count");
                            for(int x=0;x<queueList.size();x++){
                                if(queueList.get(x).tag.equals("0")){
                                    tempList1.add(queueList.get(x));
                                }
                            }

                            if(!tempList1.isEmpty()){
                                for(int x=0;x<tempList1.size();x++){
                                    Log.i(TAG,tempList1.get(x).data+":"+tempList1.get(x).proposedSeq+":"+tempList1.get(x).tag
                                            +":"+tempList1.get(x).processId);
                                }
                            }

                            for(int x=0;x<tempList1.size();x++){
                                A: for(int j=0;j<queueList.size();j++){
                                    if(queueList.get(j)== tempList1.get(x)){
                                        queueList.remove(j);
                                        break A;
                                    }
                                }
                            }

                            Log.i(TAG,"LIST AFTER FINAL UPDATE");
                            for(int x=0;x<queueList.size();x++){
                                Log.i(TAG,queueList.get(x).data+":"+queueList.get(x).proposedSeq+":"+queueList.get(x).tag
                                        +":"+queueList.get(x).processId);
                            }

                            while(!queueList.isEmpty()){
                                int headSeq = queueList.get(0).getProposedSeq();
                                for(int x=0;x<queueList.size();x++){
                                    if(queueList.get(x).proposedSeq==headSeq){
                                        tempList.add(queueList.get(x));
                                    }
                                }

                                for(int x=0;x<tempList.size();x++){
                                    if(tempList.get(x).tag.equals("1")){
                                        tempCount++;
                                    }
                                }

                                if(tempCount==tempList.size()){
                                    for(int x=0;x<tempList.size();x++){
                                        ContentValues keyValueToInsert = new ContentValues();
                                        keyValueToInsert.put("key", Integer.toString(count));
                                        keyValueToInsert.put("value", queueList.get(0).data);
                                        getContentResolver().insert(providerUri, keyValueToInsert);
                                        publishProgress(queueList.get(0).data + ":" + count);
                                        count++;
                                        queueList.remove(0);
                                    }
                                }

                                tempCount=0;
                                tempList.clear();
                            }
                        }
                    }

                    int counter3 = 0;
                    for(int x=0;x<processList.size();x++){
                        if(processList.get(x)==5){
                            counter3++;
                        }
                    }

                    Log.i(TAG, "FINAL MAIN "+REMOTE_PORTS.size()+" "+counter3+" "+failedNode);
                    if((REMOTE_PORTS.size()==5 && failedNode==true
                            && counter3==4 && !queueList.isEmpty())){
                        Log.i(TAG,"Final Main Count");
                        for(int x=0;x<queueList.size();x++){
                            if(queueList.get(x).tag.equals("0")){
                                tempList1.add(queueList.get(x));
                            }
                        }

                        if(!tempList1.isEmpty()){
                            for(int x=0;x<tempList1.size();x++){
                                Log.i(TAG,tempList1.get(x).data+":"+tempList1.get(x).proposedSeq+":"+tempList1.get(x).tag
                                        +":"+tempList1.get(x).processId);
                            }
                        }

                        for(int x=0;x<tempList1.size();x++){
                            A: for(int j=0;j<queueList.size();j++){
                                if(queueList.get(j)== tempList1.get(x)){
                                    queueList.remove(j);
                                    break A;
                                }
                            }
                        }

                        Log.i(TAG,"LIST AFTER FINAL UPDATE");
                        for(int x=0;x<queueList.size();x++){
                            Log.i(TAG,queueList.get(x).data+":"+queueList.get(x).proposedSeq+":"+queueList.get(x).tag
                                    +":"+queueList.get(x).processId);
                        }

                        while(!queueList.isEmpty()){
                            int headSeq = queueList.get(0).getProposedSeq();
                            for(int x=0;x<queueList.size();x++){
                                if(queueList.get(x).proposedSeq==headSeq){
                                    tempList.add(queueList.get(x));
                                }
                            }

                            for(int x=0;x<tempList.size();x++){
                                if(tempList.get(x).tag.equals("1")){
                                    tempCount++;
                                }
                            }

                            if(tempCount==tempList.size()){
                                for(int x=0;x<tempList.size();x++){
                                    ContentValues keyValueToInsert = new ContentValues();
                                    keyValueToInsert.put("key", Integer.toString(count));
                                    keyValueToInsert.put("value", queueList.get(0).data);
                                    getContentResolver().insert(providerUri, keyValueToInsert);
                                    publishProgress(queueList.get(0).data + ":" + count);
                                    count++;
                                    queueList.remove(0);
                                }
                            }

                            tempCount=0;
                            tempList.clear();
                        }
                    }

                    dOS.flush();
                    dIS.close();
                    dOS.close();
                    s1.close();

                } catch (SocketException e) {
                    Log.e(TAG,"Node failed Socket Exception");
                } catch (IOException e) {
                    Log.e(TAG,"Node failed");
                    REMOTE_PORTS.remove(processid);
                    Log.e(TAG,"Server Task detected failure");
                }

            }

            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "GroupMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }

//----------------------------------------------------------------------------------------------------------------------------------------------------

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            CTCount++;

            for(int i=0;i<2;i++) {
                Iterator<String> iterator = REMOTE_PORTS.iterator();
                while(iterator.hasNext()){
                    String remotePort = iterator.next();
                    try {
                        Log.e(TAG,"Client Task");
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));  //client socket connects to server socket by connecting to ip:port
                        String msgToSend = msgs[0];
                        DataInputStream dIS = new DataInputStream(socket.getInputStream());
                        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

                        if(i==1){        //Second iteration, broadcasting the agreed upon sequence number
                            if(broadcast==null){
                                int id = Integer.parseInt(msgs[1]);
                                for(int j=0;j<list1.size();j++){
                                    Log.i(TAG,list1.get(j));
                                }
                                for(int j=0;j<list2.size();j++){
                                    Log.i(TAG,Integer.toString(list2.get(j)));
                                }

                                int maxSeq = Collections.max(list2);
                                int index = list2.indexOf(maxSeq);
                                String idMaxP = list1.get(index);  //Id of the avd that suggested the max sequence number
                                Log.i(TAG,"avd which suggested max seq is: "+idMaxP+" and maxseq is: "+maxSeq);
                                broadcast = msgToSend+":"+maxSeq+":"+"Suggested by "+":"+idMaxP+":"+"from"+":"+id;
                                list1.clear();
                                list2.clear();
                            }

                            Log.i(TAG,"Second Iteration!: "+broadcast+" to "+remotePort);
                            dOut.writeUTF(broadcast);
                            String ack;
                            ack = dIS.readUTF();

                            if(ack.equals("Ack")){
                                socket.close();
                                Log.i(TAG,ack + "Received");
                                Log.i(TAG,"Socket closed");
                            }

                        } else{
                            int id = Integer.parseInt(msgs[1]);        //id is the avd from which the message is being sent
                            int sendId = Integer.parseInt(remotePort);   //sendId is the avd to which the message is to be sent
                            String msgToSend1 = msgToSend+":"+id+":"+sendId;
                            Log.i(TAG,msgToSend1);
                            dOut.writeUTF(msgToSend1); //First we write
                            Log.e(TAG,"Message Sent");
                            dOut.flush();

                            String temp = dIS.readUTF(); //Then we read
                            Log.i(TAG,"proposal received to "+temp);
                            String[] response = temp.split(":");
                            list1.add(response[2]);    //id from which sequence number is received
                            list2.add(Integer.parseInt(response[1]));  //The sequence number suggested by other avd

                            Log.i(TAG,"List sizes: "+Integer.toString(list1.size())+" "+Integer.toString(list2.size()));

                            if(list2.size()==REMOTE_PORTS.size()){  //Finding max sequence number after reply from all avds
                                for(int j=0;j<list1.size();j++){
                                    Log.i(TAG,list1.get(j));
                                }
                                for(int j=0;j<list2.size();j++){
                                    Log.i(TAG,Integer.toString(list2.get(j)));
                                }

                                int maxSeq = Collections.max(list2);
                                int index = list2.indexOf(maxSeq);
                                String idMaxP = list1.get(index);  //Id of the avd that suggested the max sequence number
                                Log.i(TAG,"avd which suggested max seq is: "+idMaxP+" and maxseq is: "+maxSeq);
                                broadcast = msgToSend+":"+maxSeq+":"+"Suggested by "+":"+idMaxP+":"+"from"+":"+id;
                                list1.clear();
                                list2.clear();
                            }

                        }

                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (SocketTimeoutException e) {
                        Log.e(TAG,"Node failed Socket Exception");
                    } catch (IOException e) {
                        Log.e(TAG,"Node failed");
                        if(REMOTE_PORTS.size()==5){       //If avd goes down then remove from REMOTE_PORTS so further messages aren't sent to that avd
                            iterator.remove();
                            if(i==0)
                                broadcast = null;
                        }
                        Log.i(TAG,"Port "+remotePort+" removed");
                        Log.i(TAG,"REMOTE_PORTS SIZE "+REMOTE_PORTS.size());
                    }

                }
            }

            if(CTCount == 5 && REMOTE_PORTS.size()==4){
                try{
                    for(String remotePort: REMOTE_PORTS){
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        DataInputStream dIS = new DataInputStream(socket.getInputStream());
                        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
                        dOut.writeUTF("Node:failed");
                        String ack;
                        ack = dIS.readUTF();
                        Log.i(TAG,"Final failed node notification sent");
                    }

                } catch(IOException e){

                }
            }

            return null;
        }

    }

}

//-----------------------------------------------------------------------------------------------------------------------------------------------------