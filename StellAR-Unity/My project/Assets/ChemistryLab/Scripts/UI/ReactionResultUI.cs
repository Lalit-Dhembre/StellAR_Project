using UnityEngine;
using UnityEngine.UI;
using TMPro;
using System.Collections;

namespace ChemistryLab.UI
{
    using Core;

    /// <summary>
    /// Displays reaction results on screen when reactions occur
    /// </summary>
    public class ReactionResultUI : MonoBehaviour
    {
        [Header("Settings")]
        [SerializeField] private float displayDuration = 4f;
        [SerializeField] private float fadeSpeed = 2f;

        private Canvas _canvas;
        private GameObject _resultPanel;
        private TextMeshProUGUI _equationText;
        private TextMeshProUGUI _descriptionText;
        private TextMeshProUGUI _effectText;
        private Image _panelBackground;
        private CanvasGroup _canvasGroup;
        private Coroutine _hideCoroutine;

        private void Start()
        {
            CreateUI();
            
            // Subscribe to reaction events
            if (ReactionEngine.Instance != null)
            {
                ReactionEngine.Instance.OnReactionOccurred += OnReaction;
            }
        }

        private void OnDestroy()
        {
            if (ReactionEngine.Instance != null)
            {
                ReactionEngine.Instance.OnReactionOccurred -= OnReaction;
            }
        }

        private void CreateUI()
        {
            // Find or create canvas
            _canvas = FindAnyObjectByType<Canvas>();
            if (_canvas == null)
            {
                var canvasObj = new GameObject("ReactionResultCanvas");
                _canvas = canvasObj.AddComponent<Canvas>();
                _canvas.renderMode = RenderMode.ScreenSpaceOverlay;
                _canvas.sortingOrder = 200;
                canvasObj.AddComponent<CanvasScaler>();
                canvasObj.AddComponent<GraphicRaycaster>();
            }

            // Create result panel
            _resultPanel = new GameObject("ReactionResultPanel");
            _resultPanel.transform.SetParent(_canvas.transform);

            RectTransform panelRect = _resultPanel.AddComponent<RectTransform>();
            panelRect.anchorMin = new Vector2(0.1f, 0.4f);
            panelRect.anchorMax = new Vector2(0.9f, 0.65f);
            panelRect.sizeDelta = Vector2.zero;
            panelRect.anchoredPosition = Vector2.zero;

            _panelBackground = _resultPanel.AddComponent<Image>();
            _panelBackground.color = new Color(0.05f, 0.1f, 0.15f, 0.95f);

            _canvasGroup = _resultPanel.AddComponent<CanvasGroup>();

            var layout = _resultPanel.AddComponent<VerticalLayoutGroup>();
            layout.padding = new RectOffset(20, 20, 15, 15);
            layout.spacing = 10;
            layout.childAlignment = TextAnchor.MiddleCenter;
            layout.childForceExpandHeight = false;

            // Equation text (large, bold)
            var eqObj = new GameObject("EquationText");
            eqObj.transform.SetParent(_resultPanel.transform);
            var eqRect = eqObj.AddComponent<RectTransform>();
            eqRect.sizeDelta = new Vector2(0, 45);
            _equationText = eqObj.AddComponent<TextMeshProUGUI>();
            _equationText.fontSize = 32;
            _equationText.fontStyle = FontStyles.Bold;
            _equationText.alignment = TextAlignmentOptions.Center;
            _equationText.color = Color.white;
            var eqLayout = eqObj.AddComponent<LayoutElement>();
            eqLayout.preferredHeight = 45;

            // Description text (medium)
            var descObj = new GameObject("DescriptionText");
            descObj.transform.SetParent(_resultPanel.transform);
            var descRect = descObj.AddComponent<RectTransform>();
            descRect.sizeDelta = new Vector2(0, 35);
            _descriptionText = descObj.AddComponent<TextMeshProUGUI>();
            _descriptionText.fontSize = 20;
            _descriptionText.alignment = TextAlignmentOptions.Center;
            _descriptionText.color = new Color(0.9f, 0.9f, 0.9f);
            var descLayout = descObj.AddComponent<LayoutElement>();
            descLayout.preferredHeight = 35;

            // Effect text (with emoji)
            var effectObj = new GameObject("EffectText");
            effectObj.transform.SetParent(_resultPanel.transform);
            var effectRect = effectObj.AddComponent<RectTransform>();
            effectRect.sizeDelta = new Vector2(0, 30);
            _effectText = effectObj.AddComponent<TextMeshProUGUI>();
            _effectText.fontSize = 24;
            _effectText.alignment = TextAlignmentOptions.Center;
            _effectText.color = new Color(1f, 0.8f, 0.3f);
            var effectLayout = effectObj.AddComponent<LayoutElement>();
            effectLayout.preferredHeight = 30;

            // Start hidden
            _resultPanel.SetActive(false);
        }

        private void OnReaction(ReactionResult result)
        {
            if (result == null || !result.isSuccessful) return;
            if (result.reaction == null) return;

            // Update UI
            _equationText.text = result.reaction.GetEquation();
            _descriptionText.text = result.reaction.description;
            
            // Effect with emoji
            string effectStr = GetEffectString(result.primaryEffect);
            if (result.allEffects != null && result.allEffects.Count > 1)
            {
                foreach (var effect in result.allEffects)
                {
                    if (effect != result.primaryEffect)
                        effectStr += " + " + GetEffectString(effect);
                }
            }
            _effectText.text = effectStr;

            // Set panel color based on effect
            _panelBackground.color = GetPanelColor(result.primaryEffect);

            // Show panel
            if (_hideCoroutine != null)
                StopCoroutine(_hideCoroutine);
            
            _resultPanel.SetActive(true);
            _canvasGroup.alpha = 1f;
            
            _hideCoroutine = StartCoroutine(HideAfterDelay());
        }

        private string GetEffectString(Data.ReactionEffectType effect)
        {
            return effect switch
            {
                Data.ReactionEffectType.GasEvolution => "💨 Gas Evolution",
                Data.ReactionEffectType.Heat => "🔥 Heat Released",
                Data.ReactionEffectType.Explosion => "💥 EXPLOSION!",
                Data.ReactionEffectType.Flame => "🔥 Fire!",
                Data.ReactionEffectType.Precipitate => "⬇️ Precipitate Forms",
                Data.ReactionEffectType.ColorChange => "🎨 Color Change",
                Data.ReactionEffectType.Fizzing => "🫧 Fizzing",
                Data.ReactionEffectType.Foam => "🐘 Elephant's Toothpaste!",
                Data.ReactionEffectType.Smoke => "💨 Smoke",
                _ => "⚗️ Reaction"
            };
        }

        private Color GetPanelColor(Data.ReactionEffectType effect)
        {
            return effect switch
            {
                Data.ReactionEffectType.Explosion => new Color(0.3f, 0.1f, 0.05f, 0.95f),
                Data.ReactionEffectType.Flame => new Color(0.25f, 0.1f, 0.02f, 0.95f),
                Data.ReactionEffectType.Heat => new Color(0.2f, 0.1f, 0.05f, 0.95f),
                Data.ReactionEffectType.GasEvolution => new Color(0.1f, 0.15f, 0.2f, 0.95f),
                Data.ReactionEffectType.Precipitate => new Color(0.1f, 0.12f, 0.18f, 0.95f),
                Data.ReactionEffectType.ColorChange => new Color(0.15f, 0.1f, 0.2f, 0.95f),
                Data.ReactionEffectType.Foam => new Color(0.1f, 0.3f, 0.5f, 0.95f),
                Data.ReactionEffectType.Fizzing => new Color(0.1f, 0.2f, 0.2f, 0.95f),
                Data.ReactionEffectType.Smoke => new Color(0.2f, 0.2f, 0.2f, 0.95f),
                _ => new Color(0.05f, 0.1f, 0.15f, 0.95f)
            };
        }

        private IEnumerator HideAfterDelay()
        {
            yield return new WaitForSeconds(displayDuration);

            // Fade out
            while (_canvasGroup.alpha > 0)
            {
                _canvasGroup.alpha -= Time.deltaTime * fadeSpeed;
                yield return null;
            }

            _resultPanel.SetActive(false);
        }
    }
}
