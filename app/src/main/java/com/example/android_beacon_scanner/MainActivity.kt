package com.example.android_beacon_scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_beacon_scanner.ui.theme.AndroidbeaconscannerTheme
import org.altbeacon.beacon.Beacon

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidbeaconscannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    BeaconList(beacons = BeaconList)
                }
            }
        }
    }
}



@Composable
fun BeaconList(beacons: List<Beacon>) {
    LazyColumn {
        items(beacons) { beacon ->
            // 각 비콘 정보를 화면에 표시하는 Compose 컴포넌트를 호출
            BeaconItem(beacon)
        }
    }
}

@Composable
fun BeaconItem(beacon: Beacon) {
    // 비콘 정보를 화면에 표시하는 UI 구성
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("UUID: ${beacon.uuid}")
            Text("Major: ${beacon.major}")
            Text("Minor: ${beacon.minor}")
            Text("Distance: ${beacon.distance} meters")
        }
    }
}

data class Beacon(
    val uuid: String,
    val major: Int,
    val minor: Int,
    val distance: Double
)


class BeaconViewModel : ViewModel() {
    // Beacon 스캔 결과를 저장하는 LiveData나 State를 선언
    val beaconData: MutableState<List<Beacon>> = mutableStateOf(emptyList())

    // Beacon 스캔을 시작하는 함수
    fun startScanning() {
        // Beacon 스캔을 시작하고 결과를 beaconData에 업데이트
        // AltBeacon 라이브러리를 사용하여 Beacon 스캔 코드 작성
    }
}




@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidbeaconscannerTheme {
    }
}