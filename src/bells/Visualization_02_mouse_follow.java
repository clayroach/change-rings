package bells;

//Visualization_02

//ORIGINAL CODE FROM: http://bgo.la/soundandvision.pde
//CHANGE RINGS FROM: http://www.changeringing.co.uk/handbells.htm  --THERE ARE MORE!

//COMMENTS IN CAPS ARE MINE

import processing.core.PApplet;
import processing.opengl.*;
import ddf.minim.*;
import ddf.minim.analysis.*;

public class Visualization_02_mouse_follow extends PApplet {

	public boolean sketchFullScreen() {
		return true;
	}

	Minim minim;
	AudioPlayer song;
	FFT fft;
	AudioInput in;

	int c1;
	int RASTRO = 10;
	float[] bands;
	int v;

	// ...............................................................
	// SETUP..............

	public void setup() {
		// strokeWeight(0);
		size(displayWidth, displayHeight, P2D);
		// size(900, 900, P2D); //(700, 800, P2D);
		minim = new Minim(this);

		// song = minim.loadFile("sabre_dance.mp3", 2048); // VARIOUS MUSIC
		// SELECTIONS
		// song = minim.loadFile("sabre_dance_slow.mp3", 2048);
		song = minim.loadFile("5040gr15b.mp3", 2048); // MY OTHER FAVORITE
		// song = minim.loadFile("5184lo08a.mp3", 2048);

		// song = minim.loadFile("12345st11c.mp3", 2048); // NEW FAVORITE
		// song = minim.loadFile("5080lo10a.mp3", 2048); // MY FAVORITE
		// song = minim.loadFile("5080lo10a_slow.mp3", 2048);
		/*
		 * if (mousePressed) { song.loop(); }
		 */
		// colorMode(HSB); THIS COLOR MODE DRIVES ME CRAZY -- I SHOULD LEARN
		// THIS....
		ellipseMode(CENTER);
		rectMode(CORNER);

		// get a line in from Minim, default bit depth is 16
		in = minim.getLineIn(Minim.STEREO, 512);
		v = 0;
		// fft = new FFT(in.bufferSize(), in.sampleRate());
		fft = new FFT(song.bufferSize(), song.sampleRate());
		fft.window(FFT.HAMMING);

		// initialize peak-hold structures - I TOOK THIS OUT BECAUSE I COULDN'T
		// FIGURE OUT WHAT IT DID
		/*
		 * peaksize = 1+Math.round(fft.specSize()/binsperband); peaks = new
		 * float[peaksize]; peak_age = new int[peaksize];
		 */
		frameRate(30);
	}

	// //////////////////////////////////////////////////////DRAW LOOP
	// ///////////////////

	public void draw() {
		// .......................................................................................

		noStroke(); // FADE BACKGROUND
		fill(0, 20); // 10);
		rectMode(CORNER);
		rect(0, 0, width, height);

		// MUSIC DOENS'T START UNTIL MOUSE PRESS
		if (mousePressed) {
			song.loop();
		}
		c1 = color(random(255), random(129), random(247));
		// .......................................................................................

		float[] bands = new float[32];

		/*
		 * if (v%RASTRO == 0) { background(0); }
		 */
		v++;
		fft.forward(song.mix);
		// fft.forward(in.mix);
		for (int i = 0; i < fft.specSize() / 2; i++) {
			bands[i / ((fft.specSize() / 2) / (bands.length - 1))] += fft
					.getBand(i);
		}
		// float total = 0.0; WHY IS THIS HERE??????? (IN THE ORIGINAL) DOING
		// NOTHING??

		// translate(width/20, height/2); //height/1.1); // MORE INTERESTING
		// WHEN IMAGE IS OFF CENTER
		float band_m0 = (bands[0] / fft.specSize() / 2);
		float band_m1 = (bands[1] / fft.specSize() / 2);
		float band_m2 = (bands[2] / fft.specSize() / 2) * 10;
		float band_m3 = (bands[3] / fft.specSize() / 2) * 10;
		float band_m4 = (bands[4] / fft.specSize() / 2) * 10;
		float band_m5 = (bands[5] / fft.specSize() / 2) * 10;

		if (bands[0] > 25) {
			strokeWeight(2);
			stroke(255, 255, 0);
			// noFill();
			fill(255 * band_m0, 180, 255, 130); // BLUE //255, 200); // AQUA //,
												// 100);
			ellipse(pmouseX, pmouseY, bands[0] * 4, bands[0] * 4); // 0, 0,
																	// bands[0]*10,
																	// bands[0]*10);
		}
		if (bands[1] > 20) {
			strokeWeight(15);
			stroke(255, 197, 80); // , 125);
			noFill();
			rotate(PI / random(5));
			triangle(-10 * bands[1], -10 * bands[1], mouseX, 10 * bands[1],
					10 * bands[1], -10 * bands[1]);
			// triangle(random(-width/2, width/2), random(-height/2, height/2),
			// band_m1*28, band_m1*28,10*bands[1], -10*bands[1]);
		}
		if (bands[2] > 5) {
			strokeWeight(10);
			stroke(255 * band_m2, 100, 155); // , 75);
			noFill();
			// fill(255*band_m2, 200, 155, 75);
			rotate(PI / random(3));
			rect(mouseX, mouseY, 15 * bands[2], 15 * bands[2]);
		} /*
		 * if (bands[3] > 5) { strokeWeight(band_m3); //*50); //*100); SPIRAL
		 * BANDS stroke (0); //(255*band_m3, 255, 255); //255, 255, 255);
		 * //*band_m3, 255, 255); //255, 128); //, 100); //fill(255, 255, 255,
		 * 150); // 255*band_m3, 255, 128, 200); //70); //ellipse(0, 0,
		 * 15*bands[2], 15*bands[2]); //spiral(500*band_m3, bands[3],
		 * (int)(band_m3*2*width)); spiral(pmouseX, pmouseY,
		 * (int)(band_m3*2*width)); //50*band_m3, bands[3],
		 * (int)(band_m3*2*width)); strokeWeight(0); }
		 */
		if (bands[4] > 1) {
			strokeWeight(3);
			stroke(255, 0, 0);
			fill(c1); // 255, 129, 247, 180); // ROSE-COLORED SQUARES //
						// (255*band_m4, 255, 255, 200);
			rect(random(width / 2), random(height / 2), band_m4 * 280,
					band_m4 * 280); // random(-width/2, width/2),
									// random(-height/2, height/2), band_m4*280,
									// band_m4*280);
		}
		if (bands[5] > 0.5) {
			rectMode(CENTER);
			strokeWeight(2);
			stroke(0);
			fill(254, 255, 9); // YELLOW SQUARES //255*band_m5);
			rect(width / 2, height / 2, band_m5 * 250, band_m5 * 250); // random(-width/2,
																		// width/2),
																		// random(-height/2,
																		// height/2),
																		// band_m5*250,
																		// band_m5*250);
			rectMode(CORNER);
		}

	}

	void spiral(float segments, float N_voltas, int raio_max) {
		int t, raio;
		float cx, cy, px0 = 0, py0 = 0, px1 = 0, py1 = 0;
		float angle;
		for (t = 0; t < segments; t++) {
			cx = 0;
			cy = 0;
			raio = (int) ((t / segments) * raio_max);
			angle = (float) ((t / segments) * 2 * 3.1415 * N_voltas);
			px1 = cx + raio * cos(angle);
			py1 = cy + raio * sin(angle);
			line(px0, py0, px1, py1);
			px0 = px1;
			py0 = py1;
		}
		// saveFrame("frames/lines-#######.tif"); THIS IS FOR MAKING A VIDEO
		// WITH MOVIE MAKER
	}

	public void stop() {
		in.close();
		minim.stop();

		super.stop();
	}

	public void keyPressed() {
		exit();
	}
}