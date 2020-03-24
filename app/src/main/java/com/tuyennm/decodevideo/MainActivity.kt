package com.tuyennm.decodevideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.io.DataInputStream
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    //    http://xhelmboyx.tripod.com/formats/mp4-layout.txt
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //test
        val inputStream = DataInputStream(assets.open("samplevideo_1280x720_5mb.mp4"))
        println(inputStream.readInt())
        println(ReadUtils.readString(inputStream, 4))
        println(ReadUtils.readString(inputStream, 4))
        println(inputStream.readInt())
        println(ReadUtils.readString(inputStream, 4))
        println(ReadUtils.readString(inputStream, 4))
        println(ReadUtils.readString(inputStream, 4))
        println(ReadUtils.readString(inputStream, 4))
        println(inputStream.readInt())
        println(ReadUtils.readString(inputStream, 4))
        println(inputStream.readInt())
        println(inputStream.readInt())



        inputStream.close()

    }
}
