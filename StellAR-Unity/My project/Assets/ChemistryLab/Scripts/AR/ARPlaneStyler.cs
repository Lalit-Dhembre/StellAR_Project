using UnityEngine;
using UnityEngine.XR.ARFoundation;
using System.Collections.Generic;

namespace ChemistryLab.AR
{
    /// <summary>
    /// Improves AR Plane visuals by applying a procedural grid texture and handling fading.
    /// Eliminates the need for external texture assets.
    /// </summary>
    [RequireComponent(typeof(ARPlaneManager))]
    public class ARPlaneStyler : MonoBehaviour
    {
        [Header("Style Settings")]
        [SerializeField] private Color gridColor = new Color(0.2f, 0.8f, 1f, 0.5f);
        [SerializeField] private Color fillColor = new Color(0.1f, 0.4f, 0.8f, 0.1f);
        [SerializeField] private float gridSize = 10f; // Tiling factor

        private ARPlaneManager _planeManager;
        private Material _planeMaterial;
        private Texture2D _gridTexture;

        private void Awake()
        {
            _planeManager = GetComponent<ARPlaneManager>();
            CreateGridMaterial();
        }

        private void OnEnable()
        {
            _planeManager.planesChanged += OnPlanesChanged;
        }

        private void OnDisable()
        {
            _planeManager.planesChanged -= OnPlanesChanged;
        }

        private void CreateGridMaterial()
        {
            // Generate a grid texture procedurally
            int res = 256;
            _gridTexture = new Texture2D(res, res);
            _gridTexture.wrapMode = TextureWrapMode.Repeat;
            _gridTexture.filterMode = FilterMode.Bilinear;

            Color[] pixels = new Color[res * res];
            int border = 4; // Thickness

            for (int y = 0; y < res; y++)
            {
                for (int x = 0; x < res; x++)
                {
                    bool isBorder = x < border || x > res - border || y < border || y > res - border;
                    
                    // Add a center dot
                    bool isDot = Mathf.Abs(x - res/2) < border && Mathf.Abs(y - res/2) < border;

                    if (isBorder || isDot)
                        pixels[y * res + x] = Color.white;
                    else
                        pixels[y * res + x] = new Color(1, 1, 1, 0.1f); // Slight fill
                }
            }
            _gridTexture.SetPixels(pixels);
            _gridTexture.Apply();

            // Create material
            // Attempt to use URP Unlit, fall back to Standard
            Shader shader = Shader.Find("Universal Render Pipeline/Unlit");
            if (shader == null) shader = Shader.Find("Unlit/Transparent");
            
            _planeMaterial = new Material(shader);
            _planeMaterial.mainTexture = _gridTexture;
            _planeMaterial.color = gridColor;
            
            // Set transparency mode if using Standard shader
            if (shader.name == "Standard")
            {
                _planeMaterial.SetFloat("_Mode", 3);
                _planeMaterial.SetInt("_SrcBlend", (int)UnityEngine.Rendering.BlendMode.SrcAlpha);
                _planeMaterial.SetInt("_DstBlend", (int)UnityEngine.Rendering.BlendMode.OneMinusSrcAlpha);
                _planeMaterial.EnableKeyword("_ALPHABLEND_ON");
                _planeMaterial.renderQueue = 3000;
            }
            
            // Texture tiling
            _planeMaterial.mainTextureScale = new Vector2(gridSize, gridSize);
        }

        private void OnPlanesChanged(ARPlanesChangedEventArgs args)
        {
            // Apply material to new planes
            foreach (var plane in args.added)
            {
                ApplyStyle(plane);
            }

            foreach (var plane in args.updated)
            {
                ApplyStyle(plane);
            }
        }

        private void ApplyStyle(ARPlane plane)
        {
            var renderer = plane.GetComponent<MeshRenderer>();
            if (renderer != null)
            {
                renderer.sharedMaterial = _planeMaterial;
                
                // Adjust tiling based on plane size if needed
                // But usually fixed tiling is better for AR context
            }

            // Disable line renderer if present (default AR plane uses it)
            var lineRenderer = plane.GetComponent<LineRenderer>();
            if (lineRenderer != null)
            {
                lineRenderer.enabled = false;
            }
        }
    }
}
