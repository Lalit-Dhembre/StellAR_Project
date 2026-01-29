using UnityEngine;
using UnityEngine.UI;
using TMPro;
using System.Collections.Generic;

namespace ChemistryLab.UI
{
    using Data;
    using Core;
    using Containers;

    /// <summary>
    /// Main UI manager for the Chemistry Lab
    /// </summary>
    public class ChemistryLabUIManager : MonoBehaviour
    {
        [Header("Panels")]
        [SerializeField] private GameObject mainPanel;
        [SerializeField] private GameObject chemicalSelectionPanel;
        [SerializeField] private GameObject reactionInfoPanel;
        [SerializeField] private GameObject controlsPanel;

        [Header("Chemical Selection")]
        [SerializeField] private Transform chemicalButtonContainer;
        [SerializeField] private GameObject chemicalButtonPrefab;

        [Header("Reaction Info")]
        [SerializeField] private TextMeshProUGUI equationText;
        [SerializeField] private TextMeshProUGUI reactionDescriptionText;
        [SerializeField] private Image reactionEffectIcon;

        [Header("Controls")]
        [SerializeField] private Slider temperatureSlider;
        [SerializeField] private TextMeshProUGUI temperatureText;
        [SerializeField] private TextMeshProUGUI pHText;
        [SerializeField] private Image pHIndicator;

        [Header("Bottom Toolbar")]
        [SerializeField] private Button chemicalsButton;
        [SerializeField] private Button equipmentButton;
        [SerializeField] private Button clearButton;

        [SerializeField] private Button infoButton;
        [SerializeField] private Button deleteButton; // New Delete Button

        [Header("References")]
        [SerializeField] private ChemicalDatabase database;
        [SerializeField] private Interaction.LabTouchInteraction labInteraction;

        // Currently selected container
        private ChemicalContainer _selectedContainer;
        private List<Button> _chemicalButtons = new List<Button>();

        private void Start()
        {
            SetupButtons();
            PopulateChemicalButtons();
            SubscribeToEvents();
            
            // Hide panels initially
            if (chemicalSelectionPanel != null)
                chemicalSelectionPanel.SetActive(false);
            if (reactionInfoPanel != null)
                reactionInfoPanel.SetActive(false);
        }

        private void SetupButtons()
        {
            if (chemicalsButton != null)
                chemicalsButton.onClick.AddListener(ToggleChemicalPanel);
            if (equipmentButton != null)
                equipmentButton.onClick.AddListener(OnEquipmentButtonClicked);
            if (clearButton != null)
                clearButton.onClick.AddListener(OnClearButtonClicked);
            if (infoButton != null)
                infoButton.onClick.AddListener(ToggleInfoPanel);
            
            if (deleteButton != null && labInteraction != null)
                deleteButton.onClick.AddListener(labInteraction.DeleteSelectedObject);
            
            if (temperatureSlider != null)
                temperatureSlider.onValueChanged.AddListener(OnTemperatureChanged);
        }

        private void PopulateChemicalButtons()
        {
            // Try to find database if missing
            if (database == null && ReactionEngine.Instance != null)
            {
                database = ReactionEngine.Instance.Database;
            }

            if (database == null || chemicalButtonContainer == null || chemicalButtonPrefab == null)
                return;

            // Clear existing
            foreach (Transform child in chemicalButtonContainer)
            {
                Destroy(child.gameObject);
            }
            _chemicalButtons.Clear();

            // Create button for each chemical
            foreach (var chemical in database.chemicals)
            {
                if (chemical == null) continue;

                GameObject btnObj = Instantiate(chemicalButtonPrefab, chemicalButtonContainer);
                Button btn = btnObj.GetComponent<Button>();

                // Set button text
                var text = btnObj.GetComponentInChildren<TextMeshProUGUI>();
                if (text != null)
                {
                    text.text = chemical.formula;
                }

                // Set button color based on chemical
                var image = btnObj.GetComponent<Image>();
                if (image != null && chemical.color.a > 0.1f)
                {
                    Color btnColor = chemical.color;
                    btnColor.a = 1f;
                    image.color = btnColor;
                }

                // Add click handler
                ChemicalData chem = chemical; // Capture for closure
                btn.onClick.AddListener(() => OnChemicalSelected(chem));

                _chemicalButtons.Add(btn);
            }
        }

        private void SubscribeToEvents()
        {
            if (ReactionEngine.Instance != null)
            {
                ReactionEngine.Instance.OnReactionOccurred += OnReaction;
            }
        }

        private void Update()
        {
            UpdateContainerInfo();
        }

        private void UpdateContainerInfo()
        {
            if (_selectedContainer == null) return;

            // Update temperature display
            if (temperatureText != null)
            {
                temperatureText.text = $"{_selectedContainer.Temperature:F1}°C";
            }

            // Update pH display
            if (pHText != null)
            {
                pHText.text = $"pH: {_selectedContainer.CurrentPH:F1}";
            }

            // Update pH indicator color
            if (pHIndicator != null)
            {
                float pH = _selectedContainer.CurrentPH;
                if (pH < 7)
                    pHIndicator.color = Color.Lerp(Color.red, Color.yellow, pH / 7f);
                else if (pH > 7)
                    pHIndicator.color = Color.Lerp(Color.green, Color.blue, (pH - 7f) / 7f);
                else
                    pHIndicator.color = Color.green;
            }
        }

        /// <summary>
        /// Toggle chemical selection panel visibility
        /// </summary>
        public void ToggleChemicalPanel()
        {
            if (chemicalSelectionPanel != null)
            {
                chemicalSelectionPanel.SetActive(!chemicalSelectionPanel.activeSelf);
            }
        }

        /// <summary>
        /// Toggle reaction info panel visibility
        /// </summary>
        public void ToggleInfoPanel()
        {
            if (reactionInfoPanel != null)
            {
                reactionInfoPanel.SetActive(!reactionInfoPanel.activeSelf);
            }
        }

        /// <summary>
        /// Called when a chemical is selected from the panel
        /// </summary>
        public void OnChemicalSelected(ChemicalData chemical)
        {
            if (_selectedContainer != null)
            {
                _selectedContainer.AddChemical(chemical, 10f); // Add 10mL
            }
            else
            {
                Debug.Log($"No container selected. Tap a container first to add {chemical.chemicalName}");
            }
        }

        /// <summary>
        /// Set the currently selected container
        /// </summary>
        public void SelectContainer(ChemicalContainer container)
        {
            _selectedContainer = container;
            
            if (controlsPanel != null)
                controlsPanel.SetActive(container != null);
        }

        /// <summary>
        /// Get the currently selected container
        /// </summary>
        public ChemicalContainer SelectedContainer => _selectedContainer;

        /// <summary>
        /// Called when equipment button is clicked
        /// </summary>
        private void OnEquipmentButtonClicked()
        {
            // Spawn equipment at camera forward position
            if (Equipment.ProceduralEquipmentGenerator.Instance != null)
            {
                Vector3 spawnPos = Camera.main.transform.position + Camera.main.transform.forward * 0.5f;
                spawnPos.y = 0; // Ground level
                
                // Cycle through equipment types
                Equipment.ProceduralEquipmentGenerator.Instance.CreateBeaker(null);
            }
        }

        /// <summary>
        /// Clear all contents from selected container
        /// </summary>
        private void OnClearButtonClicked()
        {
            if (_selectedContainer != null)
            {
                _selectedContainer.Empty();
            }
        }

        /// <summary>
        /// Handle temperature slider change
        /// </summary>
        private void OnTemperatureChanged(float value)
        {
            // Temperature slider controls bunsen burner
            // Could trigger heating of selected container
        }

        /// <summary>
        /// Called when a reaction occurs
        /// </summary>
        private void OnReaction(ReactionResult result)
        {
            if (result == null || !result.isSuccessful) return;

            // Show reaction info
            if (reactionInfoPanel != null)
                reactionInfoPanel.SetActive(true);

            if (equationText != null && result.reaction != null)
                equationText.text = result.reaction.GetEquation();

            if (reactionDescriptionText != null && result.reaction != null)
                reactionDescriptionText.text = result.reaction.description;
        }

        private void OnDestroy()
        {
            if (ReactionEngine.Instance != null)
            {
                ReactionEngine.Instance.OnReactionOccurred -= OnReaction;
            }
        }
    }
}
