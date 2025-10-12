# Guía de la Nueva Funcionalidad en la GUI

## 🎯 Nueva Funcionalidad: Conversión Infijo → Prefijo

Se ha agregado un nuevo botón en la interfaz gráfica del compilador de Haskell:

### 🔘 **Botón: "Conversión Infijo→Prefijo"**

#### **Ubicación**: Panel derecho de la GUI, junto a los otros botones de análisis

#### **Funcionalidad**:
- Analiza automáticamente el código fuente en busca de expresiones aritméticas
- Convierte las expresiones encontradas a notación prefijo
- Genera código intermedio en formato de tripletas y cuádruplos
- Muestra resultados detallados y formatéados

## 📋 **¿Cómo usar la nueva funcionalidad?**

### **Paso 1**: Escribir código con expresiones aritméticas
Ejemplo:
```haskell
resultado = a + b * c
calculo = (x + y) * z
potencia = base ^ exponente
```

### **Paso 2**: Hacer clic en "Conversión Infijo→Prefijo"

### **Paso 3**: Ver los resultados en el panel de salida

## 🎨 **Formato de Salida**

La funcionalidad muestra:

### **Para cada expresión encontrada:**
- **Representación Original**: La expresión como aparece en el código
- **Notación Prefijo**: El resultado de la conversión (ej: `*+ABC`)
- **Tripletas**: Código intermedio formato `(operador, arg1, arg2, resultado)`
- **Cuádruplos**: Código intermedio formato `(operador, arg1, arg2, resultado)`

### **Ejemplo de salida:**
```
--- EXPRESIÓN 1 ---
Representación Original: (a + b) * c
Notación Prefijo: *+abc
Tripletas:
  1: (+, a, b, t1)
  2: (*, t1, c, t2)
Resultado final: t2
Cuádruplos:
  1: (+, a, b, t1)
  2: (*, t1, c, t2)
Resultado final: t2
```

## 🔍 **Casos Especiales**

### **Cuando NO hay expresiones aritméticas:**
- La GUI muestra un mensaje informativo
- Proporciona ejemplos de expresiones que se pueden probar
- Incluye una demostración automática con expresiones de ejemplo

### **Cuando hay errores sintácticos:**
- Muestra mensaje de error detallado
- Sugiere ejecutar primero el "Análisis Sintáctico"
- Proporciona posibles causas del problema

## 🚀 **Características Técnicas**

### **Algoritmo Integrado:**
- Utiliza el algoritmo de Shunting Yard modificado
- Preserva todas las características del algoritmo original
- Maneja correctamente la precedencia de operadores (+, -, *, /, ^)
- Soporta asociatividad (derecha para ^, izquierda para otros)

### **Código Intermedio:**
- **Tripletas**: Más compactas, útiles para optimización
- **Cuádruplos**: Más explícitos, útiles para generación de código
- Ambos formatos utilizan variables temporales (t1, t2, etc.)

### **Detección Automática:**
- Busca expresiones aritméticas en todo el AST del programa
- Utiliza reflexión para acceder a nodos del parser
- Procesa múltiples expresiones en el mismo archivo

## 📁 **Archivos de Ejemplo**

Se ha incluido `ejemplos_expresiones.hs` que contiene:
- Expresiones simples: `a + b`, `x * y`
- Expresiones con precedencia: `a + b * c`
- Expresiones con paréntesis: `(a + b) * c`
- Expresiones complejas: `(a + b) * (c - d)`
- Casos especiales y expresiones anidadas

## 🎯 **Beneficios de la Nueva Funcionalidad**

1. **Visualización Interactiva**: Ver inmediatamente cómo se convierten las expresiones
2. **Aprendizaje**: Entender el algoritmo de conversión paso a paso
3. **Debugging**: Verificar que las expresiones se interpreten correctamente
4. **Código Intermedio**: Ver cómo se genera el código de 3 direcciones
5. **Integración Completa**: Funciona con el lexer y parser existentes

## ✅ **Verificación de Funcionamiento**

Para probar la funcionalidad:
1. Abrir la GUI del compilador
2. Cargar o escribir código con expresiones aritméticas
3. Hacer clic en "Conversión Infijo→Prefijo"
4. Verificar que aparezcan los resultados formateados
5. Probar con diferentes tipos de expresiones

La nueva funcionalidad está completamente integrada y lista para usar! 🚀