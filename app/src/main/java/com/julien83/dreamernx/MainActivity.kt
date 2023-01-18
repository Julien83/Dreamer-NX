package com.julien83.dreamernx

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.AsyncTask
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

class MainActivity : AppCompatActivity() {
    private lateinit var StatusView: TextView
    private lateinit var BuseTempView: TextView
    private lateinit var BedTempView: TextView
    private lateinit var PrintProgessBar: ProgressBar
    private lateinit var ButtonConnect: Button
    private lateinit var ButtonStop: Button
    private lateinit var ipEditText: EditText
    private lateinit var portEditText: EditText
    private var runthead:Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BuseTempView = findViewById(R.id.textBuse)
        BedTempView = findViewById(R.id.textBed)
        PrintProgessBar = findViewById(R.id.progressBar)
        ButtonConnect = findViewById(R.id.buttonCONNECT)
        ButtonStop = findViewById(R.id.buttonSTOP)
        StatusView = findViewById(R.id.textStatus)


        ButtonConnect.setOnClickListener{
            //recupération des valeur de l'IP et du Port
            ipEditText = findViewById(R.id.editIP)
            portEditText = findViewById(R.id.editPORT)
            //Maj du status
            StatusView.text = "Connecting"
            // Send message to server in a background thread
            runthead = true
            SendMessageTask().execute()
        }

        ButtonStop.setOnClickListener{
            runthead = false
        }
    }
    //fun connect() {
        private inner class SendMessageTask : AsyncTask<Void, List<String>, List<String>>()
    {
        override fun doInBackground(vararg params: Void): List<String>
        {
            try {

                //recupération de l'IP et du Port de IHM
                val serverAddress = InetAddress.getByName(ipEditText.text.toString())
                val serverPort = portEditText.text.toString().toInt()
                val sockaddr: SocketAddress = InetSocketAddress(serverAddress, serverPort)
                //Overture du socket
                val socket = Socket()
                socket.connect(sockaddr,10000)
                //val soc = Socket(serverAddress, serverPort)
                try {
                    //Ouverture des flux d'entrée et de sortie du socket
                    val dataout = DataOutputStream(socket.getOutputStream())
                    val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))

                    //boucle d'envoie des trame de demande d'information
                    while(runthead){
                        //Connexion a la machine
                        dataout.writeBytes("~M601 S1\r\n")
                        dataout.flush()
                        var cpt = 0
                        while (!inputStream.ready()|| cpt< 3)
                        {
                            Thread.sleep(10)
                            cpt++
                        }
                        val response1 = inputStream.readLine()
                        val response2 = inputStream.readLine()
                        val response3 = inputStream.readLine()

                        //Avancement 3DPrint
                        dataout.writeBytes("~M27\r\n")
                        dataout.flush()
                        val response11 = inputStream.readLine()
                        val response12 = inputStream.readLine()
                        val response13 = inputStream.readLine()

                        //Temperature machine
                        dataout.writeBytes("~M105\r\n")
                        dataout.flush()
                        val response21 = inputStream.readLine()
                        val response22 = inputStream.readLine()
                        val response23 = inputStream.readLine()

                        // Demande Statut
                        /*dataout.writeBytes("~M119\r\n")
                        dataout.flush()
                        val response31 = inputStream.readLine()
                        val response32 = inputStream.readLine()
                        val response33 = inputStream.readLine()
                        val response34 = inputStream.readLine()*/


                        var printingList = response12.split(" ", "/")
                        var tempList = response22.split(":", "/")


                        var listRet = listOf<String>(
                            tempList.get(1),
                            tempList.get(3),
                            printingList.get(3),
                            printingList.get(4)
                        )

                        publishProgress(listRet)
                        Thread.sleep(1000)
                    }
                    dataout.close()
                    socket.close()
                    var listOk = listOf<String>("Ok","Finish")
                    return listOk
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    var listErr = listOf<String>("Error", "Error: ${e.message}")
                    return listErr

                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                //var listErr = listOf<String>("Error", "Error: ${e.message}")
                var listErr = listOf<String>("Error", "3DPrinter No Found")
                return listErr
            }
        }

        override fun onPostExecute(listRet: List<String>) {

            StatusView.text = "OFF"
            if(listRet.get(0).equals("Error"))
            {
                val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 150)
            }

            Toast.makeText(this@MainActivity,listRet.get(1),Toast.LENGTH_SHORT).show()

        }

        override fun onProgressUpdate(vararg values: List<String>) {
            super.onProgressUpdate(*values)
            StatusView.text = "Connected"
            BuseTempView.text = values.get(0).get(0)+"°C"
            BedTempView.text = values.get(0).get(1)+"°C"
            val printingBytes = values.get(0).get(2).toInt()
            val totalBytes = values.get(0).get(3).toInt()
            if(totalBytes!=0)
            {
                val pourcent = (printingBytes * 100) / totalBytes
                PrintProgessBar.setProgress(pourcent)
                if(pourcent == 100)
                {
                    val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                }

            }
        }
    }
}