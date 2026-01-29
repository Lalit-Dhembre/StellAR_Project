using UnityEngine;

namespace ChemistryLab.Equipment
{
    /// <summary>
    /// Loads 3D models from prefabs in the project
    /// Assign prefabs in the Inspector after importing GLB models
    /// </summary>
    public class ModelLoader : MonoBehaviour
    {
        private static ModelLoader _instance;
        public static ModelLoader Instance => _instance;

        [Header("Model Prefabs - Drag your imported models here")]
        [SerializeField] private GameObject beakerPrefab;
        [SerializeField] private GameObject testTubePrefab;
        [SerializeField] private GameObject bunsenBurnerPrefab;

        [Header("Model Scale Adjustments")]
        [SerializeField] private float beakerScale = 0.03f;
        [SerializeField] private float testTubeScale = 0.1f;
        [SerializeField] private float bunsenBurnerScale = 0.4f;

        public bool IsLoaded => true; // Prefabs are always ready

        private void Awake()
        {
            _instance = this;
            
            // Try to auto-load models from Resources if not assigned
            TryLoadModels();
            LogModelStatus();
        }

        private void TryLoadModels()
        {
            // Try loading from Resources/ChemistryLab/
            if (beakerPrefab == null)
            {
                beakerPrefab = Resources.Load<GameObject>("ChemistryLab/beaker");
                if (beakerPrefab == null)
                    Debug.Log("[ModelLoader] Could not load beaker from 'ChemistryLab/beaker'");
            }
            
            if (testTubePrefab == null)
            {
                testTubePrefab = Resources.Load<GameObject>("ChemistryLab/test_tube");
                if (testTubePrefab == null)
                    Debug.Log("[ModelLoader] Could not load test_tube from 'ChemistryLab/test_tube'");
            }
            
            if (bunsenBurnerPrefab == null)
            {
                bunsenBurnerPrefab = Resources.Load<GameObject>("ChemistryLab/bunsen_burner");
                if (bunsenBurnerPrefab == null)
                    Debug.Log("[ModelLoader] Could not load bunsen_burner from 'ChemistryLab/bunsen_burner'");
            }

            // Log what was loaded
            if (beakerPrefab != null)
                Debug.Log($"[ModelLoader] ✅ Loaded beaker: {beakerPrefab.name}");
            if (testTubePrefab != null)
                Debug.Log($"[ModelLoader] ✅ Loaded test_tube: {testTubePrefab.name}");
            if (bunsenBurnerPrefab != null)
                Debug.Log($"[ModelLoader] ✅ Loaded bunsen_burner: {bunsenBurnerPrefab.name}");
        }

        private void LogModelStatus()
        {
            Debug.Log($"[ModelLoader] Status - Beaker: {(beakerPrefab != null ? "✓ LOADED" : "✗ missing")}");
            Debug.Log($"[ModelLoader] Status - Test Tube: {(testTubePrefab != null ? "✓ LOADED" : "✗ missing")}");
            Debug.Log($"[ModelLoader] Status - Bunsen Burner: {(bunsenBurnerPrefab != null ? "✓ LOADED" : "✗ missing")}");
            
            if (beakerPrefab == null && testTubePrefab == null && bunsenBurnerPrefab == null)
            {
                Debug.LogWarning("[ModelLoader] ⚠️ No models loaded! Using procedural shapes instead.");
                Debug.LogWarning("[ModelLoader] To use 3D models, place prefabs in: Assets/Resources/ChemistryLab/");
            }
        }

        /// <summary>
        /// Create a beaker instance from the prefab
        /// </summary>
        public GameObject CreateBeaker(Transform parent = null)
        {
            if (beakerPrefab == null)
            {
                Debug.LogWarning("[ModelLoader] Beaker prefab not assigned, using fallback");
                return ProceduralEquipmentGenerator.Instance?.CreateBeaker(parent);
            }

            var instance = Instantiate(beakerPrefab, parent);
            instance.name = "Beaker";
            instance.transform.localScale = Vector3.one * beakerScale;
            
            // IMPORTANT: Fix rotation for imported models - ensure upright orientation
            // Many GLB models import with wrong axis, so we force upright
            instance.transform.localRotation = Quaternion.identity;
            
            SetupAsContainer(instance, "Beaker", 250f);
            
            return instance;
        }

        /// <summary>
        /// Create a test tube instance from the prefab
        /// </summary>
        public GameObject CreateTestTube(Transform parent = null)
        {
            if (testTubePrefab == null)
            {
                Debug.LogWarning("[ModelLoader] Test tube prefab not assigned, using fallback");
                return ProceduralEquipmentGenerator.Instance?.CreateTestTube(parent);
            }

            var instance = Instantiate(testTubePrefab, parent);
            instance.name = "TestTube";
            instance.transform.localScale = Vector3.one * testTubeScale;
            
            // IMPORTANT: Fix rotation for imported models - ensure upright orientation
            instance.transform.localRotation = Quaternion.identity;
            
            SetupAsContainer(instance, "Test Tube", 50f);
            
            return instance;
        }

        /// <summary>
        /// Create a Bunsen burner instance from the prefab
        /// </summary>
        public GameObject CreateBunsenBurner(Transform parent = null)
        {
            if (bunsenBurnerPrefab == null)
            {
                Debug.LogWarning("[ModelLoader] Bunsen burner prefab not assigned, using fallback");
                var burnerObj = new GameObject("BunsenBurner");
                if (parent != null) burnerObj.transform.SetParent(parent);
                burnerObj.AddComponent<BunsenBurner>();
                return burnerObj;
            }

            var instance = Instantiate(bunsenBurnerPrefab, parent);
            instance.name = "BunsenBurner";
            instance.transform.localScale = Vector3.one * bunsenBurnerScale;
            
            // Add burner component
            if (instance.GetComponent<BunsenBurner>() == null)
            {
                instance.AddComponent<BunsenBurner>();
            }
            
            // Add collider if missing
            if (instance.GetComponent<Collider>() == null)
            {
                var col = instance.AddComponent<BoxCollider>();
                col.size = new Vector3(0.1f, 0.2f, 0.1f);
                col.center = new Vector3(0, 0.1f, 0);
            }
            
            // Add kinematic Rigidbody to prevent falling
            var rb = instance.GetComponent<Rigidbody>();
            if (rb == null)
            {
                rb = instance.AddComponent<Rigidbody>();
            }
            rb.useGravity = false;
            rb.isKinematic = true;
            
            return instance;
        }

        /// <summary>
        /// Setup a model instance as a chemical container
        /// </summary>
        private void SetupAsContainer(GameObject obj, string containerName, float capacity)
        {
            // Add container component
            var container = obj.GetComponent<Containers.ChemicalContainer>();
            if (container == null)
            {
                container = obj.AddComponent<Containers.ChemicalContainer>();
            }

            // ALWAYS add a BoxCollider on the root object for reliable touch detection
            // This ensures the LabTouchInteraction can detect and drag the object
            if (obj.GetComponent<Collider>() == null)
            {
                var boxCol = obj.AddComponent<BoxCollider>();
                // Calculate bounds from all renderers
                var renderers = obj.GetComponentsInChildren<Renderer>();
                if (renderers.Length > 0)
                {
                    Bounds bounds = renderers[0].bounds;
                    for (int i = 1; i < renderers.Length; i++)
                    {
                        bounds.Encapsulate(renderers[i].bounds);
                    }
                    // Convert to local space
                    boxCol.center = obj.transform.InverseTransformPoint(bounds.center);
                    boxCol.size = bounds.size / obj.transform.lossyScale.x; // Account for scale
                }
                else
                {
                    // Default size if no renderers
                    boxCol.size = new Vector3(0.1f, 0.15f, 0.1f);
                }
                Debug.Log($"[ModelLoader] Added BoxCollider to {obj.name}: center={boxCol.center}, size={boxCol.size}");
            }
            
            // Add kinematic Rigidbody to prevent falling and enable dragging
            var rb = obj.GetComponent<Rigidbody>();
            if (rb == null)
            {
                rb = obj.AddComponent<Rigidbody>();
            }
            rb.useGravity = false;
            rb.isKinematic = true;
            Debug.Log($"[ModelLoader] Added kinematic Rigidbody to {obj.name}");

            // Add liquid animator for basic effects
            if (obj.GetComponent<Effects.LiquidAnimator>() == null)
            {
                obj.AddComponent<Effects.LiquidAnimator>();
            }
            
            // Add enhanced liquid visualizer for realistic chemical display
            if (obj.GetComponent<Effects.EnhancedLiquidVisualizer>() == null)
            {
                obj.AddComponent<Effects.EnhancedLiquidVisualizer>();
                Debug.Log($"[ModelLoader] Added EnhancedLiquidVisualizer to {obj.name}");
            }
        }

        // Properties to check model availability
        public bool HasBeakerModel => beakerPrefab != null;
        public bool HasTestTubeModel => testTubePrefab != null;
        public bool HasBunsenBurnerModel => bunsenBurnerPrefab != null;
    }
}
