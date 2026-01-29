using UnityEngine;
using UnityEngine.UI;
using TMPro;
using System.Collections.Generic;

namespace ChemistryLab.UI
{
    /// <summary>
    /// Lab Manual / Recipe Book UI
    /// Displays a list of available experiments and instructions.
    /// </summary>
    public class LabManualUI : MonoBehaviour
    {
        private Canvas _canvas;
        private GameObject _manualPanel;
        private Transform _contentContainer;

        private void Start()
        {
            // Find canvas
            _canvas = GetComponentInParent<Canvas>();
            if (_canvas == null)
            {
                var runtimeUI = FindFirstObjectByType<ChemistryLabRuntimeUI>();
                if (runtimeUI != null) _canvas = runtimeUI.GetComponentInChildren<Canvas>();
            }

            CreateUI();
        }

        public void ToggleVisibility()
        {
            if (_manualPanel != null)
                _manualPanel.SetActive(!_manualPanel.activeSelf);
        }

        private void CreateUI()
        {
            if (_canvas == null) return;

            // Main Panel
            _manualPanel = new GameObject("LabManualPanel");
            _manualPanel.transform.SetParent(_canvas.transform);

            RectTransform panelRect = _manualPanel.AddComponent<RectTransform>();
            panelRect.anchorMin = new Vector2(0.1f, 0.1f);
            panelRect.anchorMax = new Vector2(0.9f, 0.9f);
            panelRect.offsetMin = Vector2.zero;
            panelRect.offsetMax = Vector2.zero;

            Image bg = _manualPanel.AddComponent<Image>();
            bg.color = new Color(0.1f, 0.12f, 0.15f, 0.98f);
            
            // Header
            CreateHeader();

            // Scroll View
            CreateScrollView();

            // Populate Content
            PopulateRecipes();

            // Close Button
            CreateCloseButton();

            // Start Hidden
            _manualPanel.SetActive(false);
        }

        private void CreateHeader()
        {
            GameObject header = new GameObject("Header");
            header.transform.SetParent(_manualPanel.transform);
            
            RectTransform rect = header.AddComponent<RectTransform>();
            rect.anchorMin = new Vector2(0, 0.9f);
            rect.anchorMax = new Vector2(1, 1);
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;

            TextMeshProUGUI text = header.AddComponent<TextMeshProUGUI>();
            text.text = "📘 Lab Manual & Recipes";
            text.fontSize = 28;
            text.fontStyle = FontStyles.Bold;
            text.alignment = TextAlignmentOptions.Center;
            text.color = Color.white;
        }

        private void CreateScrollView()
        {
            // Scroll View Object
            GameObject scrollObj = new GameObject("ScrollView");
            scrollObj.transform.SetParent(_manualPanel.transform);
            
            RectTransform scrollRect = scrollObj.AddComponent<RectTransform>();
            scrollRect.anchorMin = new Vector2(0.05f, 0.05f);
            scrollRect.anchorMax = new Vector2(0.95f, 0.85f);
            scrollRect.offsetMin = Vector2.zero;
            scrollRect.offsetMax = Vector2.zero;

            ScrollRect sr = scrollObj.AddComponent<ScrollRect>();
            sr.horizontal = false;
            sr.vertical = true;
            sr.scrollSensitivity = 20f;

            // Viewport
            GameObject viewport = new GameObject("Viewport");
            viewport.transform.SetParent(scrollObj.transform);
            RectTransform viewRect = viewport.AddComponent<RectTransform>();
            viewRect.anchorMin = Vector2.zero; viewRect.anchorMax = Vector2.one; viewRect.sizeDelta = Vector2.zero;
            
            // Mask needs an Image to work properly
            Image maskImg = viewport.AddComponent<Image>();
            maskImg.color = new Color(0,0,0,0f); 
            Mask mask = viewport.AddComponent<Mask>();
            mask.showMaskGraphic = false;

            sr.viewport = viewRect;

            // Content
            GameObject content = new GameObject("Content");
            content.transform.SetParent(viewport.transform);
            _contentContainer = content.transform;
            RectTransform contentRect = content.AddComponent<RectTransform>();
            contentRect.anchorMin = new Vector2(0, 1);
            contentRect.anchorMax = new Vector2(1, 1);
            contentRect.pivot = new Vector2(0.5f, 1);
            contentRect.sizeDelta = new Vector2(0, 1000); // Will adjust

            sr.content = contentRect;

            VerticalLayoutGroup layout = content.AddComponent<VerticalLayoutGroup>();
            layout.spacing = 15;
            layout.padding = new RectOffset(10, 10, 10, 10);
            layout.childControlHeight = true;
            layout.childControlWidth = true;
            layout.childForceExpandHeight = false;
            layout.childForceExpandWidth = true;
            
            ContentSizeFitter fitter = content.AddComponent<ContentSizeFitter>();
            fitter.verticalFit = ContentSizeFitter.FitMode.PreferredSize;
        }

        private void CreateCloseButton()
        {
            GameObject btnObj = new GameObject("CloseButton");
            btnObj.transform.SetParent(_manualPanel.transform);
            
            RectTransform rect = btnObj.AddComponent<RectTransform>();
            rect.anchorMin = new Vector2(0.9f, 0.9f);
            rect.anchorMax = new Vector2(1, 1);
            rect.sizeDelta = new Vector2(50, 50);
            rect.anchoredPosition = new Vector2(-20, -20);

            Image img = btnObj.AddComponent<Image>();
            img.color = new Color(0.8f, 0.2f, 0.2f); // Red

            Button btn = btnObj.AddComponent<Button>();
            btn.targetGraphic = img;
            btn.onClick.AddListener(ToggleVisibility);

            GameObject textObj = new GameObject("Text");
            textObj.transform.SetParent(btnObj.transform);
            RectTransform textRect = textObj.AddComponent<RectTransform>();
            textRect.anchorMin = Vector2.zero; textRect.anchorMax = Vector2.one;
            
            TextMeshProUGUI tmp = textObj.AddComponent<TextMeshProUGUI>();
            tmp.text = "X";
            tmp.fontSize = 24;
            tmp.alignment = TextAlignmentOptions.Center;
            tmp.color = Color.white;
        }

        private void PopulateRecipes()
        {
            // Category: DANGEROUS / FUN
            AddCategoryHeader("💥 Explosive & Fun");
            AddRecipe("Elephant's Toothpaste", "Creates massive foam eruption!", "2H₂O₂ + 2KI", "Add Hydrogen Peroxide (H2O2) + Potassium Iodide (KI)");
            AddRecipe("Potassium Explosion", "Violent explosion with water", "2K + 2H₂O", "Add Potassium (K) to Water (H2O)");
            AddRecipe("Sodium Reaction", "Fizzing and heat", "2Na + 2H₂O", "Add Sodium (Na) to Water (H2O)");

            // Category: BASICS
            AddCategoryHeader("🌡️ Heating & Phase Change");
            AddRecipe("Boiling Water", "Turns water to steam", "H₂O(l) → H₂O(g)", "Heat Water to 100°C using Heat Button");
            AddRecipe("Acid Fuming", "Creates corrosive fumes", "HCl(aq) → HCl(g)", "Heat Hydrochloric Acid (HCl) > 80°C");

            // Category: COLORS
            AddCategoryHeader("🎨 Color Changes");
            AddRecipe("Universal Indicator", "Changes color based on pH", "pH < 7 (Red), pH > 7 (Purple)", "Add Indicator to Acid or Base");
            AddRecipe("Precipitation", "Solid formation", "CuSO₄ + 2NaOH", "Mix Copper Sulfate + Sodium Hydroxide");
        }

        private void AddCategoryHeader(string title)
        {
            GameObject header = new GameObject("CatHeader");
            header.transform.SetParent(_contentContainer);
            
            // Ensure RectTransform exists
            RectTransform rt = header.AddComponent<RectTransform>();
            
            TextMeshProUGUI text = header.AddComponent<TextMeshProUGUI>();
            text.text = title;
            text.fontSize = 24;
            text.fontStyle = FontStyles.Bold;
            text.color = new Color(1f, 0.8f, 0.2f); // Gold
            text.alignment = TextAlignmentOptions.MidlineLeft;
            
            LayoutElement layout = header.AddComponent<LayoutElement>();
            layout.minHeight = 50;
            layout.flexibleWidth = 1;
        }

        private void CreateText(Transform parent, string content, float size, FontStyles style, Color color)
        {
            GameObject obj = new GameObject("Text");
            obj.transform.SetParent(parent);
            obj.AddComponent<RectTransform>(); // Ensure RectTransform
            
            TextMeshProUGUI txt = obj.AddComponent<TextMeshProUGUI>();
            txt.text = content;
            txt.fontSize = size;
            txt.fontStyle = style;
            txt.color = color;
            txt.alignment = TextAlignmentOptions.TopLeft;
            txt.overflowMode = TextOverflowModes.Ellipsis;
            txt.enableWordWrapping = true;
        }

        private void AddRecipe(string name, string desc, string equation, string instructions)
        {
            GameObject entry = new GameObject("Recipe_" + name);
            entry.transform.SetParent(_contentContainer);
            
            // Explicit RectTransform
            RectTransform rt = entry.AddComponent<RectTransform>();
            
            // Background (slightly brighter for visibility test)
            Image bg = entry.AddComponent<Image>();
            bg.color = new Color(0.15f, 0.15f, 0.2f, 0.95f); 

            VerticalLayoutGroup layout = entry.AddComponent<VerticalLayoutGroup>();
            layout.padding = new RectOffset(10, 10, 10, 10);
            layout.spacing = 5;
            layout.childControlWidth = true;
            layout.childControlHeight = true;
            layout.childForceExpandHeight = false;
            layout.childForceExpandWidth = true;

            // Name
            CreateText(entry.transform, name, 20, FontStyles.Bold, Color.white);
            // Equation
            CreateText(entry.transform, equation, 16, FontStyles.Italic, new Color(0.7f, 0.9f, 1f));
            // Description
            CreateText(entry.transform, desc, 16, FontStyles.Normal, new Color(0.9f, 0.9f, 0.9f));
            // Instructions
            CreateText(entry.transform, instructions, 16, FontStyles.Normal, new Color(1f, 1f, 0.6f));

            LayoutElement le = entry.AddComponent<LayoutElement>();
            le.preferredHeight = 140; // Increased height
            le.flexibleWidth = 1;
        }


    }
}
