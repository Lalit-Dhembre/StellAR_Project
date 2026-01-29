using UnityEngine;

namespace ChemistryLab.Audio
{
    /// <summary>
    /// Generates audio clips procedurally for chemical reactions.
    /// Eliminates the need for external audio assets.
    /// </summary>
    public static class ProceduralAudio
    {
        private const int SAMPLE_RATE = 44100;

        /// <summary>
        /// Generates a bubbling sound clip
        /// </summary>
        public static AudioClip CreateBubbleSound(int sampleCount = 44100)
        {
            float[] data = new float[sampleCount];
            // Bubbles: Random popped sine waves
            for (int i = 0; i < sampleCount; i++)
            {
                // Sparse random bubbles
                if (Random.value < 0.005f) 
                {
                    // Create a single bubble event
                    int bubbleLen = Random.Range(500, 2000);
                    float freq = Random.Range(300f, 800f);
                    for (int j = 0; j < bubbleLen && (i + j) < sampleCount; j++)
                    {
                        float t = (float)j / SAMPLE_RATE;
                        float envelope = Mathf.Exp(-5f * j / bubbleLen); // Decay
                        data[i + j] += Mathf.Sin(2 * Mathf.PI * freq * t) * envelope * 0.5f;
                    }
                }
            }
            
            AudioClip clip = AudioClip.Create("Bubble", sampleCount, 1, SAMPLE_RATE, false);
            clip.SetData(data, 0);
            return clip;
        }

        /// <summary>
        /// Generates an explosion sound clip
        /// </summary>
        public static AudioClip CreateExplosionSound(int sampleCount = 88200)
        {
            float[] data = new float[sampleCount];
            // Explosion: Burst of white noise + low freq decay
            for (int i = 0; i < sampleCount; i++)
            {
                float t = (float)i / SAMPLE_RATE;
                float envelope = Mathf.Exp(-4f * t); // Main decay
                
                // Noise component
                float noise = (Random.value * 2f - 1f) * envelope;
                
                // Low rumble component
                float rumble = Mathf.Sin(2 * Mathf.PI * 50f * t) * envelope * 0.5f;
                
                data[i] = Mathf.Clamp(noise + rumble, -1f, 1f);
            }
            
            AudioClip clip = AudioClip.Create("Explosion", sampleCount, 1, SAMPLE_RATE, false);
            clip.SetData(data, 0);
            return clip;
        }

        /// <summary>
        /// Generates a pouring sound clip (liquid noise)
        /// </summary>
        public static AudioClip CreatePourSound(int sampleCount = 44100)
        {
            float[] data = new float[sampleCount];
            // Pouring: Continuous colored noise
            float lastVal = 0;
            for (int i = 0; i < sampleCount; i++)
            {
                float white = Random.value * 2f - 1f;
                // Simple low-pass filter for "watery" sound
                lastVal = (lastVal + white) * 0.5f; 
                data[i] = lastVal * 0.8f;
            }
            
            AudioClip clip = AudioClip.Create("Pour", sampleCount, 1, SAMPLE_RATE, false);
            clip.SetData(data, 0);
            return clip;
        }

        /// <summary>
        /// Generates a fizzing sound clip (high freq noise)
        /// </summary>
        public static AudioClip CreateFizzSound(int sampleCount = 44100)
        {
            float[] data = new float[sampleCount];
            for (int i = 0; i < sampleCount; i++)
            {
                // High frequency noise
                data[i] = (Random.value * 2f - 1f) * 0.3f;
            }
            
            AudioClip clip = AudioClip.Create("Fizz", sampleCount, 1, SAMPLE_RATE, false);
            clip.SetData(data, 0);
            return clip;
        }
    }
}
