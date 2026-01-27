# CogniHertz

### Gamma-Frequency Sensory Stimulation — Experimental Exploration Tool

CogniHertz is an Android application designed for the personal exploration of rhythmic light and sound stimulation at 40Hz. Inspired by emerging neuroscience research, this app provides a sandbox for users to experience synchronized audiovisual patterns and observe their own subjective responses. It is not a medical device, nor is it intended for clinical or therapeutic use.

---

## Critical Safety Warning

**This app produces rhythmic flickering light and sound.**

**DO NOT USE if you have:**

* Epilepsy or a history of seizures
* Photosensitivity
* Severe light-triggered migraines

**Important Safety Clarifications:**

* **Chromatic Flicker:** The "color-only" flicker mode is provided to improve subjective visual comfort for some users. It **does not eliminate** the risk of seizure or photosensitive response.
* **Immediate Cessation:** Stop use immediately if you experience dizziness, nausea, disorientation, muscle twitching, or any visual disturbance.
* **Assumption of Risk:** Use of this software is entirely at your own risk.

---

## What This App Actually Does

CogniHertz presents precisely timed visual and audio pulses. These can produce sensory steady-state responses in the brain, meaning the visual or auditory systems may synchronize with the external rhythm while stimulation is active.

**What this app does not do:**

* It does not guarantee whole-brain gamma entrainment.
* It does not target specific deep brain regions (e.g., hippocampus).
* It does not produce proven cognitive enhancement or "learning optimization."
* It does not produce proven therapeutic or medical effects.
* It is not known to cause lasting neurological changes after a session.

---

## Science Inspiration vs. Consumer Reality

Research labs have studied 40Hz sensory stimulation (often called GENUS) in animals and early human experiments. Some mouse studies reported changes related to neurodegeneration under tightly controlled conditions using specialized, high-intensity equipment.

A smartphone or consumer headset is significantly different from laboratory hardware:

* **Visual Field:** Screen brightness and the percentage of the visual field covered are much smaller and less controlled than clinical light panels.
* **Spectral Precision:** Phone displays use broad-spectrum emitters. They cannot produce the narrow-band, medical-grade wavelengths (such as specific 530nm green) used in some studies.
* **Consistency:** Laboratory equipment is calibrated for sub-millisecond precision; consumer mobile hardware is subject to background OS tasks and varying hardware latencies.

---

## Hardware and External Displays

CogniHertz allows for connection to external displays, such as USB-C monitors or AR headsets (e.g., XReal). These are provided as experimental options only.

### AR Headsets and Wearables

* **Potential Value:** Headsets can cover a larger portion of the visual field and focus light more directly than a handheld screen, which may theoretically facilitate stronger sensory responses.
* **Limitations:** These devices are experimental consumer electronics. They introduce additional latency via the video signal chain (tethering) and may have different refresh rate behaviors that affect flicker precision. They should be used for entertainment and observation only.

### USB-C Monitors and External Displays

* **Potential Value:** Larger displays can fill the user's peripheral vision, which is highly sensitive to flicker and motion.
* **Limitations:** Most external monitors do not support 120Hz/144Hz refresh rates consistently via mobile USB-C connections. Displaying at 60Hz will result in "judder" (uneven timing), making a stable 40Hz signal impossible.

---

## Experimental Modes

These modes are provided for personal observation and subjective feedback only.

### Memory Discovery

40Hz light and sound stimulation inspired by gamma research. Any perceived changes in alertness or clarity are subjective and not medically validated.

### Learning Sandbox

Combines rhythmic stimulation with RSVP (Rapid Serial Visual Presentation) reading.

* **Note:** Extremely high RSVP speeds favor quick word recognition over deep comprehension. Do not expect typical information retention at high speeds.

### Mood Observation

40Hz stimulation delivered through the screen or supported AR glasses for subjective self-observation. This is not a mental health treatment.

### Green Light Observation

Provides a green-tinted screen inspired by migraine photophobia research.

* **Note:** Because phones cannot produce narrow-band medical wavelengths, this is a comfort exploration tool only. Even green light can cause discomfort depending on brightness and contrast.

---

## Technical Implementation and Timing

### High Refresh Rate Required

A 120Hz or 144Hz display is strictly required to approximate a clean 40Hz flicker pattern (e.g., 1 frame on, 2 frames off at 120Hz). A 60Hz display cannot mathematically represent 40Hz without significant timing errors.

### Audio-Locked Visual Timing

Visual changes are synchronized to the hardware audio clock. However, real-world perception is affected by:

* **Audio Latency:** Bluetooth headphones introduce significant delay. Wired headphones are required for the best approximation of sync.
* **Panel Response:** OLED and LCD panels have different "rise and fall" times, meaning the actual light output may not perfectly match the software command.

---

## Technical Notes and Research Context

This section provides scientific nuance for those interested in the details of sensory stimulation.

### Sensory Responses vs. Brain-Wide Entrainment

In humans, rhythmic stimulation most reliably produces:

* **SSVEP (Steady-State Visually Evoked Potentials):** Localized responses in the visual cortex.
* **ASSR (Auditory Steady-State Responses):** Localized responses in the auditory pathways.

These are sensory cortex responses. There is no definitive proof that consumer-grade sensory stimulation produces the kind of whole-brain synchronization seen in invasive animal studies.

### Display and Timing Limitations

A 120Hz display is the minimum requirement for a 40Hz signal. However, mobile displays use Pulse Width Modulation (PWM) for brightness control, which creates its own high-frequency flicker. This internal hardware behavior can interfere with the 40Hz protocol, making the stimulation an approximation rather than a laboratory-grade pulse.

### Expected Duration of Effects

Current evidence does not show lasting cognitive or neurological changes from brief sessions with consumer-grade sensory stimulation. Any effects are expected to occur only during the period of active stimulation.

---

## Legal Disclaimer

**NOT A MEDICAL DEVICE.** CogniHertz is not intended to diagnose, treat, cure, or prevent any disease. It has not been evaluated by the FDA or any other regulatory body. Use of this application for anything other than personal entertainment and curiosity is not recommended.

**Would you like me to generate a technical calibration guide to help you verify the frame-timing accuracy on your specific device?**