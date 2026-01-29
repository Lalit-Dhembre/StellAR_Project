using UnityEngine;

namespace ChemistryLab.Equipment
{
    /// <summary>
    /// Generates procedural lab equipment at runtime using Unity primitives.
    /// Use this to create placeholder models until proper 3D models are imported.
    /// </summary>
    public class ProceduralEquipmentGenerator : MonoBehaviour
    {
        [Header("Materials")]
        [SerializeField] private Material glassMaterial;
        [SerializeField] private Material metalMaterial;
        [SerializeField] private Material liquidMaterial;

        private static ProceduralEquipmentGenerator _instance;
        public static ProceduralEquipmentGenerator Instance => _instance;

        private void Awake()
        {
            _instance = this;
            CreateDefaultMaterials();
        }

        private void CreateDefaultMaterials()
        {
            // Create glass material - using Standard shader for better compatibility
            if (glassMaterial == null)
            {
                // Try URP first, fall back to Standard
                Shader shader = Shader.Find("Universal Render Pipeline/Lit");
                if (shader == null) shader = Shader.Find("Standard");
                
                glassMaterial = new Material(shader);
                glassMaterial.name = "Glass";
                
                // Set transparency
                glassMaterial.SetFloat("_Mode", 3); // Transparent mode
                glassMaterial.SetInt("_SrcBlend", (int)UnityEngine.Rendering.BlendMode.SrcAlpha);
                glassMaterial.SetInt("_DstBlend", (int)UnityEngine.Rendering.BlendMode.OneMinusSrcAlpha);
                glassMaterial.SetInt("_ZWrite", 0);
                glassMaterial.DisableKeyword("_ALPHATEST_ON");
                glassMaterial.EnableKeyword("_ALPHABLEND_ON");
                glassMaterial.DisableKeyword("_ALPHAPREMULTIPLY_ON");
                glassMaterial.color = new Color(0.8f, 0.9f, 1f, 0.4f);
                glassMaterial.renderQueue = 3000;
            }

            // Create metal material
            if (metalMaterial == null)
            {
                Shader shader = Shader.Find("Universal Render Pipeline/Lit");
                if (shader == null) shader = Shader.Find("Standard");
                
                metalMaterial = new Material(shader);
                metalMaterial.name = "Metal";
                metalMaterial.color = new Color(0.5f, 0.5f, 0.55f, 1f);
            }

            // Create liquid material - bright blue for visibility
            if (liquidMaterial == null)
            {
                Shader shader = Shader.Find("Universal Render Pipeline/Lit");
                if (shader == null) shader = Shader.Find("Standard");
                
                liquidMaterial = new Material(shader);
                liquidMaterial.name = "Liquid";
                
                // Set transparency
                liquidMaterial.SetFloat("_Mode", 3);
                liquidMaterial.SetInt("_SrcBlend", (int)UnityEngine.Rendering.BlendMode.SrcAlpha);
                liquidMaterial.SetInt("_DstBlend", (int)UnityEngine.Rendering.BlendMode.OneMinusSrcAlpha);
                liquidMaterial.SetInt("_ZWrite", 0);
                liquidMaterial.EnableKeyword("_ALPHABLEND_ON");
                liquidMaterial.color = new Color(0.3f, 0.6f, 1f, 0.7f);
                liquidMaterial.renderQueue = 3001;
            }
        }

        /// <summary>
        /// Create a procedural test tube - scaled up for visibility
        /// </summary>
        public GameObject CreateTestTube(Transform parent = null)
        {
            GameObject testTube = new GameObject("TestTube_Procedural");
            if (parent != null) testTube.transform.SetParent(parent);

            // Glass body - LARGER for visibility
            GameObject body = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            body.name = "Glass";
            body.transform.SetParent(testTube.transform);
            body.transform.localPosition = Vector3.zero;
            body.transform.localScale = new Vector3(0.03f, 0.12f, 0.03f); // Bigger
            ApplyMaterial(body, glassMaterial);

            // Rounded bottom
            GameObject bottom = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            bottom.name = "Bottom";
            bottom.transform.SetParent(testTube.transform);
            bottom.transform.localPosition = new Vector3(0, -0.12f, 0);
            bottom.transform.localScale = new Vector3(0.03f, 0.03f, 0.03f);
            ApplyMaterial(bottom, glassMaterial);
            Destroy(bottom.GetComponent<Collider>());

            // Liquid inside - colored
            GameObject liquid = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            liquid.name = "Liquid";
            liquid.transform.SetParent(testTube.transform);
            liquid.transform.localPosition = new Vector3(0, -0.03f, 0);
            liquid.transform.localScale = new Vector3(0.025f, 0.08f, 0.025f);
            var liqMat = new Material(liquidMaterial);
            liqMat.color = new Color(0.3f, 0.7f, 1f, 0.8f); // Light blue
            ApplyMaterial(liquid, liqMat);
            Destroy(liquid.GetComponent<Collider>());

            // Add collider to main body
            var mainCollider = testTube.AddComponent<CapsuleCollider>();
            mainCollider.height = 0.3f;
            mainCollider.radius = 0.02f;

            // Add Rigidbody - kinematic to prevent falling
            var rb = testTube.AddComponent<Rigidbody>();
            rb.mass = 0.05f;
            rb.useGravity = false;
            rb.isKinematic = true;

            // Add TestTube script
            var ttScript = testTube.AddComponent<Containers.TestTube>();
            
            // Store liquid renderer reference
            SetLiquidRenderer(ttScript, liquid.GetComponent<Renderer>());
            
            return testTube;
        }

        /// <summary>
        /// Create a procedural beaker - scaled up for visibility
        /// </summary>
        public GameObject CreateBeaker(Transform parent = null)
        {
            GameObject beaker = new GameObject("Beaker_Procedural");
            if (parent != null) beaker.transform.SetParent(parent);

            // Glass body - LARGER cylinder
            GameObject body = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            body.name = "Glass";
            body.transform.SetParent(beaker.transform);
            body.transform.localPosition = Vector3.zero;
            body.transform.localScale = new Vector3(0.1f, 0.08f, 0.1f); // Bigger
            ApplyMaterial(body, glassMaterial);

            // Bottom
            GameObject bottom = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            bottom.name = "Bottom";
            bottom.transform.SetParent(beaker.transform);
            bottom.transform.localPosition = new Vector3(0, -0.075f, 0);
            bottom.transform.localScale = new Vector3(0.095f, 0.005f, 0.095f);
            ApplyMaterial(bottom, glassMaterial);
            Destroy(bottom.GetComponent<Collider>());

            // Liquid inside - colored
            GameObject liquid = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            liquid.name = "Liquid";
            liquid.transform.SetParent(beaker.transform);
            liquid.transform.localPosition = new Vector3(0, -0.02f, 0);
            liquid.transform.localScale = new Vector3(0.09f, 0.05f, 0.09f);
            var liqMat = new Material(liquidMaterial);
            liqMat.color = new Color(0.2f, 0.8f, 0.4f, 0.7f); // Light green
            ApplyMaterial(liquid, liqMat);
            Destroy(liquid.GetComponent<Collider>());

            // Add collider
            var mainCollider = beaker.AddComponent<CapsuleCollider>();
            mainCollider.height = 0.18f;
            mainCollider.radius = 0.06f;

            // Add Rigidbody - kinematic
            var rb = beaker.AddComponent<Rigidbody>();
            rb.mass = 0.15f;
            rb.useGravity = false;
            rb.isKinematic = true;

            // Add Beaker script
            var beakerScript = beaker.AddComponent<Containers.Beaker>();
            SetLiquidRenderer(beakerScript, liquid.GetComponent<Renderer>());

            return beaker;
        }

        /// <summary>
        /// Create a procedural bunsen burner
        /// </summary>
        public GameObject CreateBunsenBurner(Transform parent = null)
        {
            GameObject burner = new GameObject("BunsenBurner_Procedural");
            if (parent != null) burner.transform.SetParent(parent);

            // Base - flat cylinder
            GameObject baseObj = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            baseObj.name = "Base";
            baseObj.transform.SetParent(burner.transform);
            baseObj.transform.localPosition = Vector3.zero;
            baseObj.transform.localScale = new Vector3(0.08f, 0.015f, 0.08f);
            ApplyMaterial(baseObj, metalMaterial);

            // Tube
            GameObject tube = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            tube.name = "Tube";
            tube.transform.SetParent(burner.transform);
            tube.transform.localPosition = new Vector3(0, 0.08f, 0);
            tube.transform.localScale = new Vector3(0.03f, 0.06f, 0.03f);
            ApplyMaterial(tube, metalMaterial);
            Destroy(tube.GetComponent<Collider>());

            // Top opening
            GameObject ring = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            ring.name = "Ring";
            ring.transform.SetParent(burner.transform);
            ring.transform.localPosition = new Vector3(0, 0.14f, 0);
            ring.transform.localScale = new Vector3(0.035f, 0.005f, 0.035f);
            
            // Orange ring to indicate flame position
            var ringMat = new Material(metalMaterial);
            ringMat.color = new Color(1f, 0.5f, 0.2f, 1f);
            ApplyMaterial(ring, ringMat);
            Destroy(ring.GetComponent<Collider>());

            // Flame indicator (visible when off, helps locate burner)
            GameObject flameIndicator = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            flameIndicator.name = "FlameIndicator";
            flameIndicator.transform.SetParent(burner.transform);
            flameIndicator.transform.localPosition = new Vector3(0, 0.17f, 0);
            flameIndicator.transform.localScale = new Vector3(0.02f, 0.03f, 0.02f);
            var flameMat = new Material(metalMaterial);
            flameMat.color = new Color(1f, 0.3f, 0.1f, 1f); // Orange
            ApplyMaterial(flameIndicator, flameMat);
            Destroy(flameIndicator.GetComponent<Collider>());

            // Flame point light
            GameObject lightObj = new GameObject("FlameLight");
            lightObj.transform.SetParent(burner.transform);
            lightObj.transform.localPosition = new Vector3(0, 0.18f, 0);
            var light = lightObj.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = new Color(0.2f, 0.5f, 1f);
            light.intensity = 1f;
            light.range = 0.5f;
            light.enabled = false;

            // Add collider
            var mainCollider = burner.AddComponent<BoxCollider>();
            mainCollider.size = new Vector3(0.1f, 0.18f, 0.1f);
            mainCollider.center = new Vector3(0, 0.08f, 0);

            // Add BunsenBurner script
            var bbScript = burner.AddComponent<Interaction.BunsenBurner>();

            return burner;
        }

        private void ApplyMaterial(GameObject obj, Material mat)
        {
            var renderer = obj.GetComponent<Renderer>();
            if (renderer != null && mat != null)
            {
                renderer.material = mat;
                renderer.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            }
        }

        private void SetLiquidRenderer(Containers.ChemicalContainer container, Renderer liquidRenderer)
        {
            // Use reflection to set the liquid renderer if field exists
            var field = typeof(Containers.ChemicalContainer).GetField("liquidRenderer", 
                System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
            if (field != null)
            {
                field.SetValue(container, liquidRenderer);
            }
        }

        /// <summary>
        /// Create a chemical bottle
        /// </summary>
        public GameObject CreateChemicalBottle(string label, Color chemicalColor, Transform parent = null)
        {
            GameObject bottle = new GameObject($"ChemicalBottle_{label}");
            if (parent != null) bottle.transform.SetParent(parent);

            // Body
            GameObject body = GameObject.CreatePrimitive(PrimitiveType.Capsule);
            body.name = "Body";
            body.transform.SetParent(bottle.transform);
            body.transform.localPosition = Vector3.zero;
            body.transform.localScale = new Vector3(0.04f, 0.06f, 0.04f);
            
            var bodyMat = new Material(glassMaterial);
            bodyMat.color = new Color(chemicalColor.r * 0.3f + 0.7f, 
                                       chemicalColor.g * 0.3f + 0.7f, 
                                       chemicalColor.b * 0.3f + 0.7f, 0.6f);
            ApplyMaterial(body, bodyMat);

            // Liquid inside
            GameObject liquid = GameObject.CreatePrimitive(PrimitiveType.Capsule);
            liquid.name = "Liquid";
            liquid.transform.SetParent(bottle.transform);
            liquid.transform.localPosition = new Vector3(0, -0.01f, 0);
            liquid.transform.localScale = new Vector3(0.032f, 0.045f, 0.032f);
            var liqMat = new Material(liquidMaterial);
            liqMat.color = new Color(chemicalColor.r, chemicalColor.g, chemicalColor.b, 0.9f);
            ApplyMaterial(liquid, liqMat);
            Destroy(liquid.GetComponent<Collider>());

            // Cap
            GameObject cap = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            cap.name = "Cap";
            cap.transform.SetParent(bottle.transform);
            cap.transform.localPosition = new Vector3(0, 0.07f, 0);
            cap.transform.localScale = new Vector3(0.02f, 0.015f, 0.02f);
            ApplyMaterial(cap, metalMaterial);
            Destroy(cap.GetComponent<Collider>());

            // Add collider
            var mainCollider = bottle.AddComponent<CapsuleCollider>();
            mainCollider.height = 0.15f;
            mainCollider.radius = 0.025f;

            // Add Rigidbody
            var rb = bottle.AddComponent<Rigidbody>();
            rb.mass = 0.1f;
            rb.useGravity = false;
            rb.isKinematic = true;

            return bottle;
        }
    }
}
