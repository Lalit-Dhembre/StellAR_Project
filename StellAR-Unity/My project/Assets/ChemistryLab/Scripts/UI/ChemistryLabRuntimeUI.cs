using UnityEngine;
using UnityEngine.UI;
using TMPro;

namespace ChemistryLab.UI
{
    using Data;
    using Core;
    using Containers;

    /// <summary>
    /// Creates a simple runtime UI for the Chemistry Lab
    /// </summary>
    public class ChemistryLabRuntimeUI : MonoBehaviour
    {
        [Header("References")]
        [SerializeField] private ChemicalDatabase database;
        [SerializeField] private AR.ChemistryLabARManager arManager;

        private Canvas _canvas;
        private GameObject _chemicalPanel;
        private GameObject _infoPanel;
        private ChemicalContainer _selectedContainer;
        private TextMeshProUGUI _infoText;
        
        // Heating
        private GameObject _heatButtonObj;
        private Image _heatButtonImage;
        private bool _isHeating = false;

        private LabManualUI _labManual;

        private void Update()
        {
            if (_isHeating && _selectedContainer != null)
            {
                // Apply heat: +50 degrees per second (fast heating)
                _selectedContainer.ApplyHeat(50f);
            }
        }

        private void Start()
        {
            // Find database if not assigned
            // Find database if not assigned
            if (database == null)
            {
                // Try from Resources/Active first
                var allDatabases = Resources.FindObjectsOfTypeAll<ChemicalDatabase>();
                if (allDatabases != null && allDatabases.Length > 0)
                {
                    database = allDatabases[0];
                }
                
                // If still null, try getting from ReactionEngine (which creates a fallback)
                if (database == null && ReactionEngine.Instance != null)
                {
                    database = ReactionEngine.Instance.Database;
                }

                if (database == null)
                {
                    Debug.LogWarning("[ChemistryLabUI] No ChemicalDatabase found! Run 'Chemistry Lab > Create Default Chemicals' first.");
                }
            }

            // Find AR manager
            if (arManager == null)
            {
                arManager = FindFirstObjectByType<AR.ChemistryLabARManager>();
            }

            CreateUI();

            // Subscribe to container selection
            if (arManager != null)
            {
                arManager.OnContainerSelected += OnContainerSelected;
            }

            // Subscribe to reactions
            if (ReactionEngine.Instance != null)
            {
                ReactionEngine.Instance.OnReactionOccurred += OnReaction;
            }
        }

        private void CreateUI()
        {
            // Create Canvas
            GameObject canvasObj = new GameObject("ChemistryLabUI");
            canvasObj.transform.SetParent(transform);
            _canvas = canvasObj.AddComponent<Canvas>();
            _canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            _canvas.sortingOrder = 100;

            canvasObj.AddComponent<CanvasScaler>();
            canvasObj.AddComponent<GraphicRaycaster>();

            // Create bottom toolbar
            CreateBottomToolbar();

            // Create chemical panel (hidden initially)
            CreateChemicalPanel();

            // Create info panel
            // Create info panel
            CreateInfoPanel();

            // Create heat button (hidden initially)
            CreateHeatButton();

            // Create Lab Manual
            CreateLabManual();
        }

        private void CreateLabManual()
        {
            GameObject lmObj = new GameObject("LabManualMgr");
            lmObj.transform.SetParent(transform);
            _labManual = lmObj.AddComponent<LabManualUI>();
        }

        private void ToggleLabManual()
        {
            if (_labManual != null) _labManual.ToggleVisibility();
        }

        private void CreateBottomToolbar()
        {
            // Toolbar container
            GameObject toolbar = new GameObject("Toolbar");
            toolbar.transform.SetParent(_canvas.transform);
            
            RectTransform toolbarRect = toolbar.AddComponent<RectTransform>();
            toolbarRect.anchorMin = new Vector2(0, 0);
            toolbarRect.anchorMax = new Vector2(1, 0);
            toolbarRect.pivot = new Vector2(0.5f, 0);
            toolbarRect.sizeDelta = new Vector2(0, 120);
            toolbarRect.anchoredPosition = Vector2.zero;

            Image toolbarBg = toolbar.AddComponent<Image>();
            toolbarBg.color = new Color(0.1f, 0.1f, 0.15f, 0.9f);

            // Horizontal layout
            HorizontalLayoutGroup layout = toolbar.AddComponent<HorizontalLayoutGroup>();
            layout.spacing = 20;
            layout.padding = new RectOffset(20, 20, 10, 10);
            layout.childAlignment = TextAnchor.MiddleCenter;
            layout.childForceExpandWidth = true;
            layout.childForceExpandHeight = true;

            // Chemicals button
            CreateButton(toolbar.transform, "Chemicals", new Color(0.2f, 0.6f, 0.9f), () => ToggleChemicalPanel());
            
            // Lab Manual Button
            CreateButton(toolbar.transform, "📘 Guide", new Color(0.4f, 0.3f, 0.8f), () => ToggleLabManual());

            // Add Beaker button
            CreateButton(toolbar.transform, "Add Beaker", new Color(0.4f, 0.8f, 0.4f), () => AddEquipment("beaker"));
            
            // Add Test Tube button
            CreateButton(toolbar.transform, "Add Tube", new Color(0.9f, 0.7f, 0.3f), () => AddEquipment("testtube"));
            
            // Burner Removed as per request
            // CreateButton(toolbar.transform, "🔥 Burner", new Color(0.9f, 0.4f, 0.2f), () => AddEquipment("burner"));
            
            // Clear button
            CreateButton(toolbar.transform, "Clear", new Color(0.9f, 0.3f, 0.3f), () => ClearSelected());
        }

        private void CreateButton(Transform parent, string text, Color color, System.Action onClick)
        {
            GameObject btnObj = new GameObject(text + "Button");
            btnObj.transform.SetParent(parent);

            RectTransform rect = btnObj.AddComponent<RectTransform>();
            rect.sizeDelta = new Vector2(120, 80);

            Image btnImage = btnObj.AddComponent<Image>();
            btnImage.color = color;

            Button btn = btnObj.AddComponent<Button>();
            btn.targetGraphic = btnImage;
            btn.onClick.AddListener(() => onClick());

            // Button text
            GameObject textObj = new GameObject("Text");
            textObj.transform.SetParent(btnObj.transform);
            
            RectTransform textRect = textObj.AddComponent<RectTransform>();
            textRect.anchorMin = Vector2.zero;
            textRect.anchorMax = Vector2.one;
            textRect.sizeDelta = Vector2.zero;
            textRect.anchoredPosition = Vector2.zero;

            TextMeshProUGUI tmpText = textObj.AddComponent<TextMeshProUGUI>();
            tmpText.text = text;
            tmpText.fontSize = 18;
            tmpText.alignment = TextAlignmentOptions.Center;
            tmpText.color = Color.white;
        }

        private void CreateChemicalPanel()
        {
            _chemicalPanel = new GameObject("ChemicalPanel");
            _chemicalPanel.transform.SetParent(_canvas.transform);

            RectTransform panelRect = _chemicalPanel.AddComponent<RectTransform>();
            panelRect.anchorMin = new Vector2(0, 0.15f);
            panelRect.anchorMax = new Vector2(1, 0.6f);
            panelRect.sizeDelta = Vector2.zero;
            panelRect.anchoredPosition = Vector2.zero;

            Image panelBg = _chemicalPanel.AddComponent<Image>();
            panelBg.color = new Color(0.15f, 0.15f, 0.2f, 0.95f);

            // Grid layout for chemical buttons
            GridLayoutGroup grid = _chemicalPanel.AddComponent<GridLayoutGroup>();
            grid.cellSize = new Vector2(140, 70);
            grid.spacing = new Vector2(10, 10);
            grid.padding = new RectOffset(20, 20, 20, 20);
            grid.childAlignment = TextAnchor.UpperCenter;

            // Create chemical buttons
            if (database != null)
            {
                foreach (var chemical in database.chemicals)
                {
                    if (chemical == null) continue;
                    CreateChemicalButton(chemical);
                }
            }

            _chemicalPanel.SetActive(false);
        }

        private void CreateChemicalButton(ChemicalData chemical)
        {
            GameObject btnObj = new GameObject(chemical.formula + "Button");
            btnObj.transform.SetParent(_chemicalPanel.transform);

            RectTransform rect = btnObj.AddComponent<RectTransform>();

            Image btnImage = btnObj.AddComponent<Image>();
            
            // Get a visible button color - use chemical color if visible, otherwise assign by category
            Color btnColor = GetVisibleButtonColor(chemical);
            btnImage.color = btnColor;

            Button btn = btnObj.AddComponent<Button>();
            btn.targetGraphic = btnImage;

            ChemicalData chem = chemical; // Capture for closure
            btn.onClick.AddListener(() => AddChemicalToSelected(chem));

            // Button text
            GameObject textObj = new GameObject("Text");
            textObj.transform.SetParent(btnObj.transform);
            
            RectTransform textRect = textObj.AddComponent<RectTransform>();
            textRect.anchorMin = Vector2.zero;
            textRect.anchorMax = Vector2.one;
            textRect.sizeDelta = Vector2.zero;
            textRect.anchoredPosition = Vector2.zero;

            TextMeshProUGUI tmpText = textObj.AddComponent<TextMeshProUGUI>();
            tmpText.text = $"{chemical.formula}\n<size=12>{chemical.chemicalName}</size>";
            tmpText.fontSize = 20;
            tmpText.alignment = TextAlignmentOptions.Center;
            tmpText.color = Color.white;
            tmpText.color = Color.white;
        }

        private void CreateInfoPanel()
        {
            _infoPanel = new GameObject("InfoPanel");
            _infoPanel.transform.SetParent(_canvas.transform);

            RectTransform panelRect = _infoPanel.AddComponent<RectTransform>();
            panelRect.anchorMin = new Vector2(0, 0.7f);
            panelRect.anchorMax = new Vector2(1, 1f);
            panelRect.sizeDelta = Vector2.zero;
            panelRect.anchoredPosition = Vector2.zero;

            Image panelBg = _infoPanel.AddComponent<Image>();
            panelBg.color = new Color(0.1f, 0.2f, 0.3f, 0.85f);

            // Info text
            GameObject textObj = new GameObject("InfoText");
            textObj.transform.SetParent(_infoPanel.transform);
            
            RectTransform textRect = textObj.AddComponent<RectTransform>();
            textRect.anchorMin = new Vector2(0.02f, 0.1f);
            textRect.anchorMax = new Vector2(0.98f, 0.9f);
            textRect.sizeDelta = Vector2.zero;
            textRect.anchoredPosition = Vector2.zero;

            _infoText = textObj.AddComponent<TextMeshProUGUI>();
            _infoText.text = "Tap on a container to select it, then add chemicals!";
            _infoText.fontSize = 20;
            _infoText.alignment = TextAlignmentOptions.Center;
            _infoText.color = Color.white;
        }

        private void ToggleChemicalPanel()
        {
            _chemicalPanel.SetActive(!_chemicalPanel.activeSelf);
        }

        private void AddEquipment(string type)
        {
            if (arManager != null)
            {
                arManager.SpawnEquipment(type);
                UpdateInfo($"Added new {type}!");
                return;
            }

            Vector3 pos = Camera.main.transform.position + Camera.main.transform.forward * 0.5f;
            pos.y = 0; // Place on ground level
            GameObject spawned = null;

            // Use PROCEDURAL generator FIRST - it creates better looking equipment
            // with visible liquid inside. Only fall back to 3D models if needed.
            var proceduralGen = Equipment.ProceduralEquipmentGenerator.Instance;
            var modelLoader = Equipment.ModelLoader.Instance;

            if (type == "beaker")
            {
                // Procedural first - has visible liquid inside
                if (proceduralGen != null)
                    spawned = proceduralGen.CreateBeaker(null);
                else if (modelLoader != null && modelLoader.HasBeakerModel)
                    spawned = modelLoader.CreateBeaker(null);
            }
            else if (type == "testtube")
            {
                // Procedural first - has visible liquid inside
                if (proceduralGen != null)
                    spawned = proceduralGen.CreateTestTube(null);
                else if (modelLoader != null && modelLoader.HasTestTubeModel)
                    spawned = modelLoader.CreateTestTube(null);
            }
            else if (type == "burner")
            {
                // Procedural first
                if (proceduralGen != null)
                    spawned = proceduralGen.CreateBunsenBurner(null);
                else if (modelLoader != null && modelLoader.HasBunsenBurnerModel)
                    spawned = modelLoader.CreateBunsenBurner(null);
                else
                    spawned = CreateBunsenBurner(pos);
            }
            
            if (spawned != null)
            {
                spawned.transform.position = pos;
                // Ensure upright orientation
                spawned.transform.rotation = Quaternion.identity;
                UpdateInfo($"Added {type}! Tap to select, drag to move.");
            }
        }

        private GameObject CreateBunsenBurner(Vector3 position)
        {
            var burnerObj = new GameObject("BunsenBurner");
            burnerObj.transform.position = position;
            var burner = burnerObj.AddComponent<Equipment.BunsenBurner>();
            
            // Toggle flame when clicked
            var clickHandler = burnerObj.AddComponent<ClickToToggleBurner>();
            
            return burnerObj;
        }

        private void AddChemicalToSelected(ChemicalData chemical)
        {
            if (_selectedContainer != null)
            {
                _selectedContainer.AddChemical(chemical, 20f);
                UpdateInfo($"Added {chemical.formula} to {_selectedContainer.ContainerName}");
                
                // Close panel after adding
                _chemicalPanel.SetActive(false);
            }
            else
            {
                UpdateInfo("Select a container first!");
            }
        }

        private void ClearSelected()
        {
            if (_selectedContainer != null)
            {
                _selectedContainer.Empty();
                UpdateInfo($"Cleared {_selectedContainer.ContainerName}");
            }
            
            // Deselect logic if needed, or just keep selected but emptied?
            // User button says "Clear", implies emptying content, not deselecting.
            // But if "Clear" meant "Deselect" then:
            // _selectedContainer = null;
            // if (_heatButtonObj != null) _heatButtonObj.SetActive(false);
            
            // However, typically "Clear" in this context usually means Empty container.
            // If I want to deselect, I usually tap empty space. 
            // Current code does not seem to have Deselect logic on background tap explicit here.
            // I'll stick to emptying.
            
            // But if I want to hide heat button when deselecting, where is deselect logic?
            // arManager.OnContainerSelected is event.
            // If I tap another, it switches.
            // If I effectively want to "Stop Heating" when deselecting interaction?
            // Handled in OnContainerSelected.
        }

        private void CreateHeatButton()
        {
            _heatButtonObj = new GameObject("HeatButton");
            _heatButtonObj.transform.SetParent(_canvas.transform);

            RectTransform rect = _heatButtonObj.AddComponent<RectTransform>();
            rect.anchorMin = new Vector2(1, 0); // Bottom Right
            rect.anchorMax = new Vector2(1, 0);
            rect.pivot = new Vector2(1, 0);
            rect.anchoredPosition = new Vector2(-20, 140); // Above toolbar
            rect.sizeDelta = new Vector2(100, 100);

            _heatButtonImage = _heatButtonObj.AddComponent<Image>();
            _heatButtonImage.color = Color.gray; // Default off

            Button btn = _heatButtonObj.AddComponent<Button>();
            btn.targetGraphic = _heatButtonImage;
            btn.onClick.AddListener(ToggleHeating);

            // Icon/Text
            GameObject textObj = new GameObject("Text");
            textObj.transform.SetParent(_heatButtonObj.transform);
            RectTransform textRect = textObj.AddComponent<RectTransform>();
            textRect.anchorMin = Vector2.zero; textRect.anchorMax = Vector2.one; textRect.sizeDelta = Vector2.zero;
            textRect.anchoredPosition = Vector2.zero;
            
            TextMeshProUGUI tmpText = textObj.AddComponent<TextMeshProUGUI>();
            tmpText.text = "🔥\nHeat";
            tmpText.fontSize = 24;
            tmpText.alignment = TextAlignmentOptions.Center;
            tmpText.color = Color.white;

            // Start hidden
            _heatButtonObj.SetActive(false);
        }
        
        private void ToggleHeating()
        {
            _isHeating = !_isHeating;
            _heatButtonImage.color = _isHeating ? new Color(1f, 0.3f, 0f) : Color.gray;
        }

        private void OnContainerSelected(ChemicalContainer container)
        {
            _selectedContainer = container;
            UpdateInfo($"Selected: {container.ContainerName} - Tap 'Chemicals' to add!");
            
            // Show heat button if container selected
            if (_heatButtonObj != null)
            {
                _heatButtonObj.SetActive(true);
                // Reset heating state
                _isHeating = false;
                _heatButtonImage.color = Color.gray;
            }
        }

        private void OnReaction(ReactionResult result)
        {
            if (result != null && result.isSuccessful && result.reaction != null)
            {
                UpdateInfo($"Reaction! {result.reaction.GetEquation()}");
            }
        }

        private void UpdateInfo(string message)
        {
            if (_infoText != null)
            {
                _infoText.text = message;
            }
            Debug.Log($"[ChemistryLab] {message}");
        }

        private void OnDestroy()
        {
            if (arManager != null)
            {
                arManager.OnContainerSelected -= OnContainerSelected;
            }
            if (ReactionEngine.Instance != null)
            {
                ReactionEngine.Instance.OnReactionOccurred -= OnReaction;
            }
        }

        /// <summary>
        /// Get a visible button color for a chemical - assigns distinct colors if the original is too transparent/light
        /// </summary>
        private Color GetVisibleButtonColor(ChemicalData chemical)
        {
            Color c = chemical.color;
            
            // Check if color is too transparent or too light to be visible
            bool needsNewColor = c.a < 0.5f || (c.r > 0.8f && c.g > 0.8f && c.b > 0.8f);
            
            if (needsNewColor)
            {
                // Assign distinct colors by category/name
                switch (chemical.category)
                {
                    case ChemicalCategory.Acid:
                        return new Color(0.9f, 0.3f, 0.3f, 1f); // Red
                    case ChemicalCategory.Base:
                        return new Color(0.3f, 0.5f, 0.9f, 1f); // Blue
                    case ChemicalCategory.Salt:
                        return new Color(0.7f, 0.7f, 0.8f, 1f); // Light gray-blue
                    case ChemicalCategory.Metal:
                        return new Color(0.6f, 0.6f, 0.7f, 1f); // Metallic gray
                    case ChemicalCategory.Solvent:
                        return new Color(0.2f, 0.6f, 0.9f, 1f); // Bright blue for Water!
                    case ChemicalCategory.Indicator:
                        return new Color(0.9f, 0.5f, 0.7f, 1f); // Pink
                    case ChemicalCategory.Oxidizer:
                        return new Color(0.9f, 0.5f, 0.2f, 1f); // Orange
                    case ChemicalCategory.Organic:
                        return new Color(0.5f, 0.8f, 0.4f, 1f); // Green
                    default:
                        return new Color(0.5f, 0.5f, 0.8f, 1f); // Default purple
                }
            }
            
            // Make sure alpha is fully opaque
            c.a = 1f;
            return c;
        }
    }

    /// <summary>
    /// Click handler to toggle Bunsen burner
    /// </summary>
    public class ClickToToggleBurner : MonoBehaviour
    {
        private Equipment.BunsenBurner _burner;

        private void Start()
        {
            _burner = GetComponent<Equipment.BunsenBurner>();
        }

        private void OnMouseDown()
        {
            if (_burner != null)
            {
                _burner.Toggle();
            }
        }
    }
}
