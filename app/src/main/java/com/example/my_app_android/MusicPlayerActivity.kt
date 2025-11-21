package com.example.my_app_android

import android.Manifest.permission.READ_MEDIA_AUDIO
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MusicPlayerActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var musicFiles = mutableListOf<MusicFile>()
    private var currentPosition = 0
    private var isMusicPlaying = false

    private val handler = Handler()
    private lateinit var seekBar: SeekBar

    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    private lateinit var listView: ListView

    private data class MusicFile(
        val path: String,
        val title: String
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadMusicFiles()
            setupListView()
        } else {
            Toast.makeText(this,
                "Разрешение необходимо для поиска музыки на устройстве",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        initViews()
        setupClickListeners()
        requestPermissions()
    }

    private fun initViews() {
        btnPlay = findViewById(R.id.btnPlay)
        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)
        btnNext = findViewById(R.id.btnNext)
        btnPrevious = findViewById(R.id.btnPrevious)
        seekBar = findViewById(R.id.seekBar)
        listView = findViewById(R.id.treklist)
    }

    private fun setupClickListeners() {
        btnPlay.setOnClickListener { playMusic() }
        btnPause.setOnClickListener { pauseMusic() }
        btnStop.setOnClickListener { stopMusic() }
        btnNext.setOnClickListener { playNext() }
        btnPrevious.setOnClickListener { playPrevious() }
    }

    private fun updateSeekBar() {
        handler.postDelayed({
            if (mediaPlayer?.isPlaying == true) {
                val currentPos = mediaPlayer!!.currentPosition
                seekBar.progress = currentPos
                updateSeekBar()
            }
        }, 1000)
    }

    private fun requestPermissions() {
        if (checkSelfPermission(READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            loadMusicFiles()
            setupListView()
        } else {
            requestPermissionLauncher.launch(READ_MEDIA_AUDIO)
        }
    }

    private fun loadMusicFiles() {
        musicFiles.clear()

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleColumn) ?: "Unknown Title"
                    val path = cursor.getString(dataColumn)

                    if (path != null) {
                        musicFiles.add(MusicFile(path, title))
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Нет разрешения для доступа к музыке", Toast.LENGTH_LONG).show()
        }

        if (musicFiles.isEmpty()) {
            Toast.makeText(this,
                "Музыкальные файлы не найдены",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupListView() {
        if (musicFiles.isEmpty()) return

        val displayList = musicFiles.map { it.title }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            currentPosition = position
            playSelectedTrack(position)
        }
    }

    private fun playSelectedTrack(position: Int) {
        if (position < musicFiles.size) {
            stopMusic()
            playMusicFile(musicFiles[position])
        }
    }

    private fun playMusicFile(musicFile: MusicFile) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(musicFile.path)
                setOnPreparedListener {
                    start()
                    isMusicPlaying = true
                    seekBar.max = duration
                    updateSeekBar()
                }
                setOnCompletionListener {
                    playNext()
                }
                prepareAsync()
            }

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    handler.removeCallbacksAndMessages(null)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    mediaPlayer?.seekTo(seekBar.progress)
                    updateSeekBar()
                    mediaPlayer?.start()
                    isMusicPlaying = true
                }
            })

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMusic() {
        if (musicFiles.isEmpty()) return

        if (mediaPlayer == null) {
            playSelectedTrack(currentPosition)
        } else if (!isMusicPlaying) {
            mediaPlayer?.start()
            isMusicPlaying = true
            updateSeekBar()
        }
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        isMusicPlaying = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isMusicPlaying = false
        handler.removeCallbacksAndMessages(null)
        seekBar.progress = 0
    }

    private fun playNext() {
        if (musicFiles.isEmpty()) return
        currentPosition = (currentPosition + 1) % musicFiles.size
        playSelectedTrack(currentPosition)
    }

    private fun playPrevious() {
        if (musicFiles.isEmpty()) return
        currentPosition = if (currentPosition - 1 < 0) {
            musicFiles.size - 1
        } else {
            currentPosition - 1
        }
        playSelectedTrack(currentPosition)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
        handler.removeCallbacksAndMessages(null)
    }
}