using UnityEngine;
using System.Collections;

namespace ChemistryLab.Equipment
{
    using Containers;

    /// <summary>
    /// Bunsen burner with animated flame for heating containers
    /// </summary>
    public class BunsenBurner : MonoBehaviour
    {
        [Header("Settings")]
        [SerializeField] private float heatRate = 5f; // Degrees per second
        [SerializeField] private float maxTemperature = 500f;
        [SerializeField] private float flameHeight = 0.15f;

        [Header("Flame Appearance")]
        [SerializeField] private Color innerFlameColor = new Color(0.3f, 0.5f, 1f); // Blue
        [SerializeField] private Color outerFlameColor = new Color(1f, 0.5f, 0.1f); // Orange

        private bool _isLit = false;
        private GameObject _innerFlame;
        private GameObject _outerFlame;
        private Light _flameLight;
        private ChemicalContainer _heatingContainer;

        public bool IsLit => _isLit;

        private void Start()
        {
            CreateBurnerAndFlame();
        }

        private void CreateBurnerAndFlame()
        {
            // Create burner base
            var baseObj = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            baseObj.name = "BurnerBase";
            baseObj.transform.SetParent(transform);
            baseObj.transform.localPosition = Vector3.zero;
            baseObj.transform.localScale = new Vector3(0.05f, 0.03f, 0.05f);
            baseObj.GetComponent<Renderer>().material.color = new Color(0.3f, 0.3f, 0.35f);

            // Create burner tube
            var tube = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            tube.name = "BurnerTube";
            tube.transform.SetParent(transform);
            tube.transform.localPosition = new Vector3(0, 0.08f, 0);
            tube.transform.localScale = new Vector3(0.02f, 0.05f, 0.02f);
            tube.GetComponent<Renderer>().material.color = new Color(0.25f, 0.25f, 0.3f);

            // Create flame holder (nozzle)
            var nozzle = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            nozzle.name = "Nozzle";
            nozzle.transform.SetParent(transform);
            nozzle.transform.localPosition = new Vector3(0, 0.13f, 0);
            nozzle.transform.localScale = new Vector3(0.025f, 0.01f, 0.025f);
            nozzle.GetComponent<Renderer>().material.color = new Color(0.4f, 0.35f, 0.3f);

            // Create inner flame (blue cone)
            _innerFlame = GameObject.CreatePrimitive(PrimitiveType.Capsule);
            _innerFlame.name = "InnerFlame";
            _innerFlame.transform.SetParent(transform);
            _innerFlame.transform.localPosition = new Vector3(0, 0.15f + flameHeight * 0.5f, 0);
            _innerFlame.transform.localScale = new Vector3(0.015f, flameHeight * 0.4f, 0.015f);
            Destroy(_innerFlame.GetComponent<Collider>());
            _innerFlame.GetComponent<Renderer>().material.color = innerFlameColor;

            // Create outer flame (orange outer cone)
            _outerFlame = GameObject.CreatePrimitive(PrimitiveType.Capsule);
            _outerFlame.name = "OuterFlame";
            _outerFlame.transform.SetParent(transform);
            _outerFlame.transform.localPosition = new Vector3(0, 0.15f + flameHeight * 0.6f, 0);
            _outerFlame.transform.localScale = new Vector3(0.025f, flameHeight * 0.6f, 0.025f);
            Destroy(_outerFlame.GetComponent<Collider>());
            var outerRenderer = _outerFlame.GetComponent<Renderer>();
            Color oc = outerFlameColor;
            oc.a = 0.7f;
            outerRenderer.material.color = oc;

            // Add flame light
            var lightObj = new GameObject("FlameLight");
            lightObj.transform.SetParent(transform);
            lightObj.transform.localPosition = new Vector3(0, 0.2f, 0);
            _flameLight = lightObj.AddComponent<Light>();
            _flameLight.type = LightType.Point;
            _flameLight.color = new Color(0.5f, 0.6f, 1f);
            _flameLight.range = 0.5f;
            _flameLight.intensity = 2f;

            // Start with flame off
            SetFlameVisible(false);

            // Add collider for interaction
            if (gameObject.GetComponent<Collider>() == null)
            {
                var col = gameObject.AddComponent<BoxCollider>();
                col.size = new Vector3(0.1f, 0.2f, 0.1f);
                col.center = new Vector3(0, 0.1f, 0);
            }
            
            // Add kinematic Rigidbody for dragging
            var rb = gameObject.GetComponent<Rigidbody>();
            if (rb == null)
            {
                rb = gameObject.AddComponent<Rigidbody>();
            }
            rb.useGravity = false;
            rb.isKinematic = true;
        }

        private void Update()
        {
            if (_isLit)
            {
                AnimateFlame();
                HeatNearbyContainers();
            }
        }

        private void AnimateFlame()
        {
            if (_innerFlame == null || _outerFlame == null) return;

            // Flicker effect
            float flicker = 1f + Mathf.Sin(Time.time * 15f) * 0.1f + Mathf.Sin(Time.time * 23f) * 0.05f;
            
            _innerFlame.transform.localScale = new Vector3(
                0.015f * flicker,
                flameHeight * 0.4f * flicker,
                0.015f * flicker
            );

            _outerFlame.transform.localScale = new Vector3(
                0.025f * flicker,
                flameHeight * 0.6f * flicker,
                0.025f * flicker
            );

            // Slight sway
            float sway = Mathf.Sin(Time.time * 5f) * 2f;
            _innerFlame.transform.localRotation = Quaternion.Euler(sway, 0, sway * 0.5f);
            _outerFlame.transform.localRotation = Quaternion.Euler(sway * 0.8f, 0, sway * 0.4f);

            // Light flicker
            if (_flameLight != null)
            {
                _flameLight.intensity = 2f + Mathf.Sin(Time.time * 20f) * 0.3f;
            }
        }

        private void HeatNearbyContainers()
        {
            // Find containers above the burner
            Collider[] nearby = Physics.OverlapSphere(transform.position + Vector3.up * 0.2f, 0.15f);
            
            foreach (var col in nearby)
            {
                var container = col.GetComponent<ChemicalContainer>();
                if (container != null)
                {
                    // Heat the container
                    container.Heat(heatRate * Time.deltaTime, maxTemperature);
                }
            }
        }

        /// <summary>
        /// Turn the burner on/off
        /// </summary>
        public void Toggle()
        {
            _isLit = !_isLit;
            SetFlameVisible(_isLit);
            Debug.Log($"[BunsenBurner] Flame is now {(_isLit ? "ON 🔥" : "OFF")}");
        }

        /// <summary>
        /// Light the burner
        /// </summary>
        public void Light()
        {
            _isLit = true;
            SetFlameVisible(true);
        }

        /// <summary>
        /// Extinguish the burner
        /// </summary>
        public void Extinguish()
        {
            _isLit = false;
            SetFlameVisible(false);
        }

        private void SetFlameVisible(bool visible)
        {
            if (_innerFlame != null) _innerFlame.SetActive(visible);
            if (_outerFlame != null) _outerFlame.SetActive(visible);
            if (_flameLight != null) _flameLight.enabled = visible;
        }
    }
}
