using UnityEngine;
using UnityEngine.UI;
using TMPro;

namespace ChemistryLab.UI
{
    using Containers;
    using Equipment;
    using Effects;

    /// <summary>
    /// Handles heating UI - shows Heat button when container is near burner,
    /// displays temperature, and manages the heating process.
    /// </summary>
    public class BurnerHeatingUI : MonoBehaviour
    {
        [Header("Settings")]
        [SerializeField] private float proximityThreshold = 0.5f; // 50cm - increased for easier detection
        [SerializeField] private float heatingPositionOffset = 0.15f;

        private ChemicalContainer _selectedContainer;
        private BunsenBurner _nearbyBurner;
        private bool _isHeating = false;
        private Canvas _canvas;
        private GameObject _uiPanel;
        private Button _heatButton;
        private Button _stopButton;
        private TextMeshProUGUI _temperatureText;
        private Image _temperatureBar;

        private readonly Color coldColor = new Color(0.3f, 0.5f, 1f);
        private readonly Color warmColor = new Color(1f, 0.8f, 0.3f);
        private readonly Color hotColor = new Color(1f, 0.3f, 0.1f);

        private void Start()
        {
            CreateHeatingUI();
        }

        private void CreateHeatingUI()
        {
            _canvas = FindAnyObjectByType<Canvas>();
            if (_canvas == null)
            {
                var canvasObj = new GameObject("HeatingUICanvas");
                _canvas = canvasObj.AddComponent<Canvas>();
                _canvas.renderMode = RenderMode.ScreenSpaceOverlay;
                canvasObj.AddComponent<CanvasScaler>();
                canvasObj.AddComponent<GraphicRaycaster>();
            }

            _uiPanel = new GameObject("HeatingPanel");
            _uiPanel.transform.SetParent(_canvas.transform);

            RectTransform panelRect = _uiPanel.AddComponent<RectTransform>();
            panelRect.anchorMin = new Vector2(0.5f, 0.75f);
            panelRect.anchorMax = new Vector2(0.5f, 0.75f);
            panelRect.sizeDelta = new Vector2(280, 100);
            panelRect.anchoredPosition = Vector2.zero;

            Image panelBg = _uiPanel.AddComponent<Image>();
            panelBg.color = new Color(0.1f, 0.1f, 0.15f, 0.9f);

            var layout = _uiPanel.AddComponent<VerticalLayoutGroup>();
            layout.padding = new RectOffset(10, 10, 10, 10);
            layout.spacing = 8;
            layout.childAlignment = TextAnchor.MiddleCenter;

            CreateTemperatureDisplay();
            CreateHeatButton();
            CreateStopButton();

            _uiPanel.SetActive(false);
        }

        private void CreateTemperatureDisplay()
        {
            var textObj = new GameObject("TempText");
            textObj.transform.SetParent(_uiPanel.transform);
            var textRect = textObj.AddComponent<RectTransform>();
            textRect.sizeDelta = new Vector2(260, 30);
            _temperatureText = textObj.AddComponent<TextMeshProUGUI>();
            _temperatureText.text = "🌡️ 25°C";
            _temperatureText.fontSize = 24;
            _temperatureText.fontStyle = FontStyles.Bold;
            _temperatureText.alignment = TextAlignmentOptions.Center;
            _temperatureText.color = Color.white;
        }

        private void CreateHeatButton()
        {
            var btnObj = new GameObject("HeatButton");
            btnObj.transform.SetParent(_uiPanel.transform);

            var rect = btnObj.AddComponent<RectTransform>();
            rect.sizeDelta = new Vector2(180, 35);

            var btnImg = btnObj.AddComponent<Image>();
            btnImg.color = new Color(1f, 0.4f, 0.2f);

            _heatButton = btnObj.AddComponent<Button>();
            _heatButton.targetGraphic = btnImg;
            _heatButton.onClick.AddListener(OnHeatPressed);

            var textObj = new GameObject("Text");
            textObj.transform.SetParent(btnObj.transform);
            var textRect = textObj.AddComponent<RectTransform>();
            textRect.anchorMin = Vector2.zero;
            textRect.anchorMax = Vector2.one;
            textRect.sizeDelta = Vector2.zero;

            var text = textObj.AddComponent<TextMeshProUGUI>();
            text.text = "🔥 Start Heating";
            text.fontSize = 16;
            text.alignment = TextAlignmentOptions.Center;
            text.color = Color.white;
        }

        private void CreateStopButton()
        {
            var btnObj = new GameObject("StopButton");
            btnObj.transform.SetParent(_uiPanel.transform);

            var rect = btnObj.AddComponent<RectTransform>();
            rect.sizeDelta = new Vector2(180, 35);

            var btnImg = btnObj.AddComponent<Image>();
            btnImg.color = new Color(0.5f, 0.5f, 0.55f);

            _stopButton = btnObj.AddComponent<Button>();
            _stopButton.targetGraphic = btnImg;
            _stopButton.onClick.AddListener(OnStopPressed);

            var textObj = new GameObject("Text");
            textObj.transform.SetParent(btnObj.transform);
            var textRect = textObj.AddComponent<RectTransform>();
            textRect.anchorMin = Vector2.zero;
            textRect.anchorMax = Vector2.one;
            textRect.sizeDelta = Vector2.zero;

            var text = textObj.AddComponent<TextMeshProUGUI>();
            text.text = "⏹ Stop Heating";
            text.fontSize = 16;
            text.alignment = TextAlignmentOptions.Center;
            text.color = Color.white;

            btnObj.SetActive(false);
        }

        private void Update()
        {
            CheckProximity();
            
            if (_isHeating && _selectedContainer != null)
            {
                UpdateTemperatureDisplay();
                CheckForBoiling();
            }
        }

        private void CheckProximity()
        {
            // Find selected container from LabTouchInteraction
            var touchInteraction = FindAnyObjectByType<Interaction.LabTouchInteraction>();
            if (touchInteraction != null && touchInteraction.SelectedContainer != null)
            {
                _selectedContainer = touchInteraction.SelectedContainer;
            }
            
            // Also check ChemistryLabUIManager if not found
            if (_selectedContainer == null)
            {
                var uiManager = FindAnyObjectByType<ChemistryLabUIManager>();
                if (uiManager != null && uiManager.SelectedContainer != null)
                {
                    _selectedContainer = uiManager.SelectedContainer;
                }
            }

            if (_selectedContainer == null)
            {
                if (!_isHeating) HideUI();
                return;
            }

            // Find nearest burner
            var burners = FindObjectsByType<BunsenBurner>(FindObjectsSortMode.None);
            _nearbyBurner = null;
            float nearestDist = float.MaxValue;

            foreach (var burner in burners)
            {
                float dist = Vector3.Distance(_selectedContainer.transform.position, burner.transform.position);
                if (dist < proximityThreshold && dist < nearestDist)
                {
                    nearestDist = dist;
                    _nearbyBurner = burner;
                }
            }

            if (_nearbyBurner != null || _isHeating)
            {
                ShowUI();
                UpdateTemperatureDisplay();
            }
            else
            {
                HideUI();
            }
        }

        private void ShowUI()
        {
            if (_uiPanel != null && !_uiPanel.activeSelf)
                _uiPanel.SetActive(true);

            if (_heatButton != null)
                _heatButton.gameObject.SetActive(!_isHeating);
            if (_stopButton != null)
                _stopButton.gameObject.SetActive(_isHeating);
        }

        private void HideUI()
        {
            if (_uiPanel != null && _uiPanel.activeSelf)
                _uiPanel.SetActive(false);
        }

        private void OnHeatPressed()
        {
            if (_selectedContainer == null || _nearbyBurner == null) return;

            Debug.Log($"[BurnerHeatingUI] Starting to heat {_selectedContainer.ContainerName}");

            // Move container onto burner
            Vector3 heatingPos = _nearbyBurner.transform.position + Vector3.up * heatingPositionOffset;
            _selectedContainer.transform.position = heatingPos;

            // Light the burner
            _nearbyBurner.Light();
            _isHeating = true;

            _heatButton.gameObject.SetActive(false);
            _stopButton.gameObject.SetActive(true);
        }

        private void OnStopPressed()
        {
            if (_nearbyBurner != null)
                _nearbyBurner.Extinguish();

            _isHeating = false;
            Debug.Log("[BurnerHeatingUI] Stopped heating");

            _heatButton.gameObject.SetActive(true);
            _stopButton.gameObject.SetActive(false);
        }

        private void UpdateTemperatureDisplay()
        {
            if (_selectedContainer == null || _temperatureText == null) return;

            float temp = _selectedContainer.Temperature;
            _temperatureText.text = $"🌡️ {temp:F0}°C";

            // Color based on temperature
            if (temp >= 100f)
                _temperatureText.color = hotColor;
            else if (temp >= 50f)
                _temperatureText.color = warmColor;
            else
                _temperatureText.color = Color.white;
        }

        private void CheckForBoiling()
        {
            if (_selectedContainer == null) return;
            
            var chemicals = _selectedContainer.GetChemicals();
            foreach (var chem in chemicals)
            {
                if (chem.formula == "H2O" && _selectedContainer.Temperature >= 100f)
                {
                    if (RealisticEffects.Instance != null && Random.value > 0.95f)
                    {
                        StartCoroutine(RealisticEffects.Instance.PlayRealisticBubbles(
                            _selectedContainer.transform.position + Vector3.up * 0.05f, 0.3f));
                    }
                }
            }
        }
    }
}
