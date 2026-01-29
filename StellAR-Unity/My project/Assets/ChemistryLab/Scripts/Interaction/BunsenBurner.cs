using UnityEngine;
using UnityEngine.XR.Interaction.Toolkit;
using UnityEngine.XR.Interaction.Toolkit.Interactables;

namespace ChemistryLab.Interaction
{
    using Containers;

    /// <summary>
    /// Bunsen burner heat source for heating containers
    /// </summary>
    public class BunsenBurner : MonoBehaviour
    {
        [Header("Heat Settings")]
        [SerializeField] private float heatRadius = 0.15f;
        [SerializeField] private float heatRate = 10f; // Degrees per second
        [SerializeField] private float maxTemperature = 500f;
        
        [Header("Flame")]
        [SerializeField] private ParticleSystem flameParticles;
        [SerializeField] private Light flameLight;
        [SerializeField] private Color normalFlameColor = new Color(0.2f, 0.5f, 1f);
        [SerializeField] private float flameIntensity = 1f;
        
        [Header("State")]
        [SerializeField] private bool isOn = false;
        
        [Header("Audio")]
        [SerializeField] private AudioSource burnerAudio;
        [SerializeField] private AudioClip igniteSound;
        [SerializeField] private AudioClip burnSound;

        private Color _currentFlameColor;

        private void Start()
        {
            _currentFlameColor = normalFlameColor;
            UpdateFlameState();
        }

        private void Update()
        {
            if (!isOn) return;

            // Find and heat nearby containers
            Collider[] nearby = Physics.OverlapSphere(transform.position + Vector3.up * 0.1f, heatRadius);
            foreach (var col in nearby)
            {
                var container = col.GetComponent<ChemicalContainer>();
                if (container != null)
                {
                    container.Heat(heatRate * Time.deltaTime);
                    
                    // Check for flame test
                    CheckFlameTest(container);
                }
            }
        }

        /// <summary>
        /// Turn the burner on
        /// </summary>
        public void TurnOn()
        {
            if (isOn) return;
            isOn = true;
            
            if (igniteSound != null && burnerAudio != null)
            {
                burnerAudio.PlayOneShot(igniteSound);
            }
            
            UpdateFlameState();
        }

        /// <summary>
        /// Turn the burner off
        /// </summary>
        public void TurnOff()
        {
            if (!isOn) return;
            isOn = false;
            _currentFlameColor = normalFlameColor;
            UpdateFlameState();
        }

        /// <summary>
        /// Toggle the burner on/off
        /// </summary>
        public void Toggle()
        {
            if (isOn) TurnOff();
            else TurnOn();
        }

        /// <summary>
        /// Check if a container above creates a flame test color
        /// </summary>
        private void CheckFlameTest(ChemicalContainer container)
        {
            if (Core.ReactionEngine.Instance == null) return;

            var chemicals = container.GetChemicals();
            foreach (var chem in chemicals)
            {
                if (chem.hasFlameColor)
                {
                    // Blend towards the flame test color
                    _currentFlameColor = Color.Lerp(_currentFlameColor, chem.flameColor, Time.deltaTime * 2f);
                    UpdateFlameColor();
                    return;
                }
            }

            // Fade back to normal
            _currentFlameColor = Color.Lerp(_currentFlameColor, normalFlameColor, Time.deltaTime);
            UpdateFlameColor();
        }

        private void UpdateFlameState()
        {
            if (flameParticles != null)
            {
                if (isOn)
                {
                    var main = flameParticles.main;
                    main.startColor = _currentFlameColor;
                    flameParticles.Play();
                }
                else
                {
                    flameParticles.Stop();
                }
            }

            if (flameLight != null)
            {
                flameLight.enabled = isOn;
                flameLight.color = _currentFlameColor;
                flameLight.intensity = isOn ? flameIntensity : 0f;
            }

            if (burnerAudio != null)
            {
                if (isOn && burnSound != null)
                {
                    burnerAudio.clip = burnSound;
                    burnerAudio.loop = true;
                    burnerAudio.Play();
                }
                else
                {
                    burnerAudio.Stop();
                }
            }
        }

        private void UpdateFlameColor()
        {
            if (flameParticles != null && isOn)
            {
                var main = flameParticles.main;
                main.startColor = _currentFlameColor;
            }

            if (flameLight != null && isOn)
            {
                flameLight.color = _currentFlameColor;
            }
        }

        public bool IsOn => isOn;
        public Color FlameColor => _currentFlameColor;

        private void OnDrawGizmosSelected()
        {
            Gizmos.color = Color.red;
            Gizmos.DrawWireSphere(transform.position + Vector3.up * 0.1f, heatRadius);
        }
    }
}
