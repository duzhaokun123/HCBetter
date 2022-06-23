package io.github.duzhaokun123.hcbetter

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class TestActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        findViewById<Button>(R.id.btn_test).setOnClickListener {
            Toast.makeText(this, "test", Toast.LENGTH_SHORT).show()
        }
    }
}