using UnityEngine;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;
using UnityEngine.XR.Interaction.Toolkit.Interactables;
using UnityEngine.XR.Interaction.Toolkit.AR;
using UnityEngine.InputSystem;
using UnityEngine.InputSystem.EnhancedTouch;
using System.Collections.Generic;
using Touch = UnityEngine.InputSystem.EnhancedTouch.Touch;

namespace ChemistryLab.AR
{
    using Data;
    using Core;
    using Containers;
    using Equipment;

    /// <summary>
    /// Main AR manager for the Chemistry Lab scene.
    /// Handles surface detection, lab placement, and AR interactions.
    /// </summary>
    public class ChemistryLabARManager : MonoBehaviour
    {
        [Header("AR Components")]
        [SerializeField] private ARPlaneManager planeManager;
        [SerializeField] private ARRaycastManager raycastManager;
        [SerializeField] private Camera arCamera;

        [Header("Lab Setup")]
        [SerializeField] private GameObject labTablePrefab;
        [SerializeField] private ChemicalDatabase chemicalDatabase;
        [SerializeField] private float labPlacementHeight = 0f;

        [Header("Equipment Prefabs")]
        [SerializeField] private GameObject testTubePrefab;
        [SerializeField] private GameObject beakerPrefab;
        [SerializeField] private GameObject bunsenBurnerPrefab;
        
        [Header("Spawn Settings")]
        [SerializeField] private bool useProceduralEquipment = false; // Use ModelLoader prefabs by default
        [SerializeField] private float equipmentSpacing = 0.15f;
        [SerializeField] private int initialTestTubes = 2;
        [SerializeField] private int initialBeakers = 1;

        [Header("Interaction")]
        [SerializeField] private float tapRadius = 0.05f;

        [Header("UI")]
        [SerializeField] private GameObject placementIndicator;
        [SerializeField] private UI.ChemistryLabUIManager uiManager;

        // State
        private bool _labPlaced = false;
        private Transform _labAnchor;
        private List<ChemicalContainer> _spawnedContainers = new List<ChemicalContainer>();
        private static List<ARRaycastHit> _raycastHits = new List<ARRaycastHit>();

        // Events
        public System.Action OnLabPlaced;
        public System.Action<ChemicalContainer> OnContainerSelected;

        private void OnEnable()
        {
            EnhancedTouchSupport.Enable();
        }

        private void OnDisable()
        {
            EnhancedTouchSupport.Disable();
        }

        private void Start()
        {
            if (arCamera == null)
                arCamera = Camera.main;

            // Initialize database
            if (chemicalDatabase != null)
                chemicalDatabase.Initialize();

            // Get AR components if not assigned
            if (planeManager == null)
                planeManager = FindFirstObjectByType<ARPlaneManager>();
            if (raycastManager == null)
                raycastManager = FindFirstObjectByType<ARRaycastManager>();

            // Show placement indicator
            if (placementIndicator != null)
                placementIndicator.SetActive(true);
        }

        private void Update()
        {
            if (!_labPlaced)
            {
                UpdatePlacementIndicator();
                CheckForPlacementTap();
            }
            else
            {
                CheckForContainerSelection();
            }
        }

        private void UpdatePlacementIndicator()
        {
            if (placementIndicator == null || raycastManager == null) return;

            Vector2 screenCenter = new Vector2(Screen.width / 2f, Screen.height / 2f);
            
            if (raycastManager.Raycast(screenCenter, _raycastHits, TrackableType.PlaneWithinPolygon))
            {
                placementIndicator.SetActive(true);
                placementIndicator.transform.position = _raycastHits[0].pose.position;
                placementIndicator.transform.rotation = _raycastHits[0].pose.rotation;
            }
            else
            {
                placementIndicator.SetActive(false);
            }
        }

        private void CheckForPlacementTap()
        {
            // Use new Input System
            if (Touch.activeTouches.Count > 0 && Touch.activeTouches[0].phase == UnityEngine.InputSystem.TouchPhase.Began)
            {
                TryPlaceLab(Touch.activeTouches[0].screenPosition);
            }
            else if (Mouse.current != null && Mouse.current.leftButton.wasPressedThisFrame)
            {
                TryPlaceLab(Mouse.current.position.ReadValue());
            }
        }

        private void TryPlaceLab(Vector2 screenPosition)
        {
            if (raycastManager == null) return;

            if (raycastManager.Raycast(screenPosition, _raycastHits, TrackableType.PlaneWithinPolygon))
            {
                var hitPose = _raycastHits[0].pose;
                PlaceLab(hitPose.position, hitPose.rotation);
            }
        }

        /// <summary>
        /// Place the lab at the specified position
        /// </summary>
        public void PlaceLab(Vector3 position, Quaternion rotation)
        {
            if (_labPlaced) return;

            // Create lab anchor
            GameObject anchor = new GameObject("LabAnchor");
            anchor.transform.position = position + Vector3.up * labPlacementHeight;
            anchor.transform.rotation = rotation;
            _labAnchor = anchor.transform;

            // Spawn lab table if we have a prefab
            if (labTablePrefab != null)
            {
                Instantiate(labTablePrefab, _labAnchor);
            }

            // NOTE: No initial equipment is spawned automatically
            // User adds equipment via UI buttons (Add Beaker, Add Tube, Burner)

            // Initialize reaction engine if needed
            if (ReactionEngine.Instance == null)
            {
                var engineObj = new GameObject("ReactionEngine");
                engineObj.transform.SetParent(_labAnchor);
                var engine = engineObj.AddComponent<ReactionEngine>();
            }

            // Hide placement indicator
            if (placementIndicator != null)
                placementIndicator.SetActive(false);

            // Keep plane visualization visible so user can see where to place equipment
            // SetPlanesVisible(false);  // Uncomment this if you want to hide planes after placing lab

            _labPlaced = true;
            OnLabPlaced?.Invoke();

            Debug.Log("Chemistry Lab placed! Use the buttons to add equipment.");
        }

        private void SpawnInitialEquipment()
        {
            // Try using ModelLoader for 3D prefabs first
            var modelLoader = ModelLoader.Instance;
            bool canUseModels = modelLoader != null && modelLoader.IsLoaded && 
                               (modelLoader.HasBeakerModel || modelLoader.HasTestTubeModel || modelLoader.HasBunsenBurnerModel);

            if (!useProceduralEquipment && canUseModels)
            {
                Debug.Log("[ChemistryLabARManager] Using ModelLoader for 3D prefabs");
                
                // Spawn test tubes
                for (int i = 0; i < initialTestTubes; i++)
                {
                    Vector3 pos = _labAnchor.position + new Vector3(-equipmentSpacing * i, 0.05f, 0);
                    var testTube = modelLoader.HasTestTubeModel 
                        ? modelLoader.CreateTestTube(_labAnchor)
                        : ProceduralEquipmentGenerator.Instance?.CreateTestTube(_labAnchor);
                    if (testTube != null)
                    {
                        testTube.transform.position = pos;
                        AddXRInteraction(testTube);
                        var container = testTube.GetComponent<ChemicalContainer>();
                        if (container != null)
                            _spawnedContainers.Add(container);
                    }
                }

                // Spawn beakers
                for (int i = 0; i < initialBeakers; i++)
                {
                    Vector3 pos = _labAnchor.position + new Vector3(equipmentSpacing * (i + 1), 0.06f, 0);
                    var beaker = modelLoader.HasBeakerModel
                        ? modelLoader.CreateBeaker(_labAnchor)
                        : ProceduralEquipmentGenerator.Instance?.CreateBeaker(_labAnchor);
                    if (beaker != null)
                    {
                        beaker.transform.position = pos;
                        AddXRInteraction(beaker);
                        var container = beaker.GetComponent<ChemicalContainer>();
                        if (container != null)
                            _spawnedContainers.Add(container);
                    }
                }

                // Spawn bunsen burner
                Vector3 burnerPos = _labAnchor.position + new Vector3(0, 0, equipmentSpacing);
                var burner = modelLoader.HasBunsenBurnerModel
                    ? modelLoader.CreateBunsenBurner(_labAnchor)
                    : ProceduralEquipmentGenerator.Instance?.CreateBunsenBurner(_labAnchor);
                if (burner != null)
                {
                    burner.transform.position = burnerPos;
                    AddXRInteraction(burner);
                }
            }
            else if (useProceduralEquipment || !canUseModels)
            {
                Debug.Log("[ChemistryLabARManager] Using ProceduralEquipmentGenerator");
                
                // Create procedural equipment generator if not exists
                var generator = ProceduralEquipmentGenerator.Instance;
                if (generator == null)
                {
                    var genObj = new GameObject("EquipmentGenerator");
                    genObj.transform.SetParent(_labAnchor);
                    generator = genObj.AddComponent<ProceduralEquipmentGenerator>();
                }

                // Spawn test tubes
                for (int i = 0; i < initialTestTubes; i++)
                {
                    Vector3 pos = _labAnchor.position + new Vector3(-equipmentSpacing * i, 0.05f, 0);
                    var testTube = generator.CreateTestTube(_labAnchor);
                    testTube.transform.position = pos;
                    AddXRInteraction(testTube);
                    
                    var container = testTube.GetComponent<ChemicalContainer>();
                    if (container != null)
                        _spawnedContainers.Add(container);
                }

                // Spawn beakers
                for (int i = 0; i < initialBeakers; i++)
                {
                    Vector3 pos = _labAnchor.position + new Vector3(equipmentSpacing * (i + 1), 0.06f, 0);
                    var beaker = generator.CreateBeaker(_labAnchor);
                    beaker.transform.position = pos;
                    AddXRInteraction(beaker);
                    
                    var container = beaker.GetComponent<ChemicalContainer>();
                    if (container != null)
                        _spawnedContainers.Add(container);
                }

                // Spawn bunsen burner
                Vector3 burnerPos = _labAnchor.position + new Vector3(0, 0, equipmentSpacing);
                var burner = generator.CreateBunsenBurner(_labAnchor);
                burner.transform.position = burnerPos;
                AddXRInteraction(burner);
            }
            else
            {
                // Use assigned prefabs
                if (testTubePrefab != null)
                {
                    for (int i = 0; i < initialTestTubes; i++)
                    {
                        Vector3 pos = _labAnchor.position + new Vector3(-equipmentSpacing * i, 0.05f, 0);
                        var obj = Instantiate(testTubePrefab, pos, Quaternion.identity, _labAnchor);
                        AddXRInteraction(obj);
                    }
                }

                if (beakerPrefab != null)
                {
                    for (int i = 0; i < initialBeakers; i++)
                    {
                        Vector3 pos = _labAnchor.position + new Vector3(equipmentSpacing * (i + 1), 0.06f, 0);
                        var obj = Instantiate(beakerPrefab, pos, Quaternion.identity, _labAnchor);
                        AddXRInteraction(obj);
                    }
                }

                if (bunsenBurnerPrefab != null)
                {
                    Vector3 pos = _labAnchor.position + new Vector3(0, 0, equipmentSpacing);
                    Instantiate(bunsenBurnerPrefab, pos, Quaternion.identity, _labAnchor);
                }
            }
        }

        private void AddXRInteraction(GameObject obj)
        {
            // Add XR Grab Interactable for manipulation
            if (obj.GetComponent<XRGrabInteractable>() == null)
            {
                var grabInteractable = obj.AddComponent<XRGrabInteractable>();
                grabInteractable.movementType = XRBaseInteractable.MovementType.VelocityTracking;
                grabInteractable.throwOnDetach = false;
            }
        }

        private void CheckForContainerSelection()
        {
            // Check for touch or click using new Input System
            bool tapped = false;
            Vector2 tapPos = Vector2.zero;

            if (Touch.activeTouches.Count > 0 && Touch.activeTouches[0].phase == UnityEngine.InputSystem.TouchPhase.Began)
            {
                tapped = true;
                tapPos = Touch.activeTouches[0].screenPosition;
            }
            else if (Mouse.current != null && Mouse.current.leftButton.wasPressedThisFrame)
            {
                tapped = true;
                tapPos = Mouse.current.position.ReadValue();
            }

            if (!tapped) return;

            // Raycast to find container
            Ray ray = arCamera.ScreenPointToRay(tapPos);
            RaycastHit hit;
            
            if (Physics.Raycast(ray, out hit, 10f))
            {
                var container = hit.collider.GetComponent<ChemicalContainer>();
                if (container != null)
                {
                    SelectContainer(container);
                }

                var burner = hit.collider.GetComponent<Interaction.BunsenBurner>();
                if (burner != null)
                {
                    burner.Toggle();
                }
            }
        }

        /// <summary>
        /// Select a container for interaction
        /// </summary>
        public void SelectContainer(ChemicalContainer container)
        {
            if (uiManager != null)
            {
                uiManager.SelectContainer(container);
            }
            OnContainerSelected?.Invoke(container);
        }

        /// <summary>
        /// Spawn a new piece of equipment
        /// </summary>
        public void SpawnEquipment(string type)
        {
            if (!_labPlaced || _labAnchor == null) return;

            Vector3 spawnPos = arCamera.transform.position + arCamera.transform.forward * 0.3f;
            spawnPos.y = _labAnchor.position.y + 0.05f;

            GameObject spawned = null;
            
            // Use PROCEDURAL generator FIRST - creates better looking equipment with liquid inside
            var proceduralGen = ProceduralEquipmentGenerator.Instance;
            var modelLoader = ModelLoader.Instance;

            switch (type.ToLower())
            {
                case "testtube":
                    // Procedural first - has visible liquid
                    if (proceduralGen != null)
                        spawned = proceduralGen.CreateTestTube(_labAnchor);
                    else if (modelLoader != null && modelLoader.HasTestTubeModel)
                        spawned = modelLoader.CreateTestTube(_labAnchor);
                    break;
                case "beaker":
                    // Procedural first - has visible liquid
                    if (proceduralGen != null)
                        spawned = proceduralGen.CreateBeaker(_labAnchor);
                    else if (modelLoader != null && modelLoader.HasBeakerModel)
                        spawned = modelLoader.CreateBeaker(_labAnchor);
                    break;
                case "burner":
                    // Procedural first
                    if (proceduralGen != null)
                        spawned = proceduralGen.CreateBunsenBurner(_labAnchor);
                    else if (modelLoader != null && modelLoader.HasBunsenBurnerModel)
                        spawned = modelLoader.CreateBunsenBurner(_labAnchor);
                    break;
            }

            if (spawned != null)
            {
                spawned.transform.position = spawnPos;
                // Ensure upright orientation
                spawned.transform.rotation = Quaternion.identity;
                AddXRInteraction(spawned);
                Debug.Log($"[ChemistryLabARManager] Spawned {type} (procedural)");
            }
        }

        /// <summary>
        /// Reset the lab
        /// </summary>
        public void ResetLab()
        {
            if (_labAnchor != null)
            {
                Destroy(_labAnchor.gameObject);
            }

            _spawnedContainers.Clear();
            _labPlaced = false;

            if (placementIndicator != null)
                placementIndicator.SetActive(true);

            SetPlanesVisible(true);
        }

        private void SetPlanesVisible(bool visible)
        {
            if (planeManager == null) return;

            foreach (var plane in planeManager.trackables)
            {
                plane.gameObject.SetActive(visible);
            }
        }

        public bool IsLabPlaced => _labPlaced;
        public Transform LabAnchor => _labAnchor;
    }
}
