---
name: Macro Tracking Design System
colors:
  surface: '#f8fafb'
  surface-dim: '#d8dadb'
  surface-bright: '#f8fafb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f5'
  surface-container: '#eceeef'
  surface-container-high: '#e6e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#3d4a3e'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#eff1f2'
  outline: '#6c7b6d'
  outline-variant: '#bbcbbb'
  surface-tint: '#006d37'
  primary: '#006d37'
  on-primary: '#ffffff'
  primary-container: '#2ecc71'
  on-primary-container: '#005027'
  inverse-primary: '#4ae183'
  secondary: '#446180'
  on-secondary: '#ffffff'
  secondary-container: '#bcdafe'
  on-secondary-container: '#43607e'
  tertiary: '#006397'
  on-tertiary: '#ffffff'
  tertiary-container: '#5fbaff'
  on-tertiary-container: '#004970'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#6bfe9c'
  primary-fixed-dim: '#4ae183'
  on-primary-fixed: '#00210c'
  on-primary-fixed-variant: '#005228'
  secondary-fixed: '#d0e4ff'
  secondary-fixed-dim: '#acc9ed'
  on-secondary-fixed: '#001d35'
  on-secondary-fixed-variant: '#2b4967'
  tertiary-fixed: '#cce5ff'
  tertiary-fixed-dim: '#92ccff'
  on-tertiary-fixed: '#001d31'
  on-tertiary-fixed-variant: '#004b73'
  background: '#f8fafb'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  title-lg:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base-unit: 4px
  margin-mobile: 16px
  gutter-grid: 12px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
---

## Brand & Style

The design system is built on a "Vitality-First" philosophy, blending the systematic precision of a data-heavy utility with the warmth of a lifestyle companion. The target audience consists of health-conscious individuals who value efficiency and clarity over complex data entry. 

The visual style is **Modern Corporate with a Health Infusion**, drawing heavily from Material 3 principles while injecting energetic bursts of color and soft, organic shapes. It leverages high-quality white space and a clear hierarchy to ensure the UI feels helpful rather than overwhelming. The emotional response should be one of "controlled momentum"—making the user feel like their health goals are both measurable (stability) and achievable (vitality).

## Colors

The palette is strategically split between action and information. 
- **Vibrant Green (Primary):** Used for growth, health indicators, success states, and primary action buttons. It represents the "energy" of the brand.
- **Deep Navy Blue (Secondary):** Used for headers, navigation, and data labels to provide a grounded, professional foundation of stability.
- **Sky Blue (Tertiary):** Used for secondary data points, such as water tracking or carb-specific breakdowns, to differentiate from the primary green.
- **Soft Grays (Neutral):** A range of cool-toned grays used for backgrounds and card surfaces to maintain a clean, Android-native feel without the harshness of pure white.

## Typography

This design system utilizes **Inter** for its exceptional legibility on mobile screens and its neutral, systematic character. 

- **Headlines:** Use tighter letter spacing and semi-bold weights to create a sense of urgency and importance in daily summaries.
- **Data Points:** Large numerals for macro counts should use the `display-lg` style to act as the primary visual anchor of the dashboard.
- **Readability:** Body text maintains a generous line height to ensure food logs and nutritional labels remain scannable during active use.

## Layout & Spacing

The layout follows a **Fluid Grid** model optimized for Android handheld devices. It utilizes a 4-column structure for mobile with 16px side margins. 

The rhythm is strictly based on a **4dp increment** system to align with Material Design standards. 
- **Vertical Rhythm:** Content blocks are separated by `stack-lg` (24px) for distinct sectioning (e.g., Breakfast vs. Lunch).
- **Internal Padding:** Cards and containers use a consistent 16px internal padding to keep content breathing.
- **Reflow:** On tablets, the single-column dashboard reflows into a 2-column layout where the "Macro Progress Rings" pin to a left-hand column while the "Food Log" scrolls on the right.

## Elevation & Depth

This design system employs **Ambient Shadows** and **Tonal Layering** to create a non-distracting hierarchy:

1.  **Level 0 (Base):** Backgrounds use the neutral `F8FAFB` gray.
2.  **Level 1 (Cards):** Primary content cards use white backgrounds with a very soft, diffused shadow (Offset: 0, 4px; Blur: 12px; Opacity: 4% Black).
3.  **Level 2 (Interactive):** Floating Action Buttons (FAB) for adding food use a slightly more pronounced shadow with a hint of the primary green tint to suggest interactivity.
4.  **Glassmorphism:** Bottom navigation bars use a subtle backdrop blur (15px) with 90% opacity to maintain context of the scrollable content behind it.

## Shapes

The shape language is **Softly Rounded**, moving away from sharp edges to feel more approachable and "human."

- **Standard Containers:** Cards and input fields utilize a 16px (`rounded-lg`) corner radius.
- **Action Elements:** Primary buttons and "Add" triggers use 24px (`rounded-xl`) or full pill-shapes to invite tapping.
- **Progress Rings:** Macro rings use rounded stroke caps to maintain consistency with the overall soft geometry.

## Components

### Progress Rings
The centerpiece of the app. Use a heavy stroke weight (8pt-12pt) for the background track in `surface_gray` and a vibrant `primary_color` for the active progress. Always use rounded caps.

### Cards
Use white surfaces on the soft gray background. Ensure a 16px corner radius. Headlines within cards should be `title-lg`.

### Buttons
- **Primary:** Filled `primary_color` with white text. High roundedness.
- **Secondary:** Outlined with `secondary_color` and a 1px border.
- **FAB:** The "Plus" button for logging food is a large circle utilizing the `primary_color` and a high-elevation shadow.

### Input Fields
Soft gray fills (`surface_gray`) with no borders in the default state. Upon focus, the field transitions to a white background with a 2px `primary_color` border.

### Food List Items
Use a standard 72dp height for list rows. Include a small circular thumbnail for food images (8px radius) and clear trailing text for calorie counts in `secondary_color`.

### Chips
Used for quick-filtering (e.g., "High Protein," "Recent," "Favorites"). These use a pill shape with a light `tertiary_color` tint and dark blue text.