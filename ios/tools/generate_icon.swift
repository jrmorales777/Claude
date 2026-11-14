import AppKit

// Premium World Voice Translator icon — globe + orbital movement + audio.
let size: CGFloat = 1024
let image = NSImage(size: NSSize(width: size, height: size))
image.lockFocus()
let full = NSRect(x: 0, y: 0, width: size, height: size)
let rounded = NSBezierPath(roundedRect: full, xRadius: 224, yRadius: 224)
let context = NSGraphicsContext.current!.cgContext
context.saveGState()
rounded.addClip()
let bg = NSGradient(starting: NSColor(calibratedRed: 0.995, green: 0.998, blue: 1, alpha: 1),
                    ending: NSColor(calibratedRed: 0.81, green: 0.89, blue: 0.99, alpha: 1))!
bg.draw(in: rounded, angle: -62)
let light = NSBezierPath(ovalIn: NSRect(x: 50, y: 110, width: 930, height: 900))
NSColor(calibratedRed: 1, green: 1, blue: 1, alpha: 0.4).setFill()
light.fill()

// The orbital ring passes behind the Earth.
let ringBack = NSBezierPath()
ringBack.move(to: NSPoint(x: 65, y: 330))
ringBack.curve(to: NSPoint(x: 946, y: 680),
               controlPoint1: NSPoint(x: 128, y: 790),
               controlPoint2: NSPoint(x: 665, y: 1020))
ringBack.lineWidth = 48
ringBack.lineCapStyle = .round
NSGradient(starting: NSColor(calibratedRed: 0.06, green: 0.66, blue: 0.99, alpha: 1),
           ending: NSColor(calibratedRed: 1, green: 0.59, blue: 0.20, alpha: 1))!
    .draw(in: ringBack, angle: 5)

// The globe has blue oceans, subtle spherical light and white continents.
let globe = NSBezierPath(ovalIn: NSRect(x: 144, y: 167, width: 755, height: 755))
let globeGradient = NSGradient(colors: [
    NSColor(calibratedRed: 0.83, green: 0.98, blue: 1, alpha: 1),
    NSColor(calibratedRed: 0.24, green: 0.71, blue: 0.98, alpha: 1),
    NSColor(calibratedRed: 0.02, green: 0.38, blue: 0.79, alpha: 1),
    NSColor(calibratedRed: 0.03, green: 0.19, blue: 0.52, alpha: 1)
])!
globeGradient.draw(in: globe, angle: 123)
context.saveGState()
globe.addClip()
func stroke(_ path: NSBezierPath, width: CGFloat, color: NSColor) {
    path.lineWidth = width
    path.lineCapStyle = .round
    color.setStroke()
    path.stroke()
}
for x in [345.0, 520.0, 695.0] {
    let p = NSBezierPath()
    p.move(to: NSPoint(x: x, y: 165))
    p.curve(to: NSPoint(x: x, y: 918), controlPoint1: NSPoint(x: x-110, y: 424), controlPoint2: NSPoint(x: x-110, y: 670))
    stroke(p, width: 5, color: NSColor.white.withAlphaComponent(0.29))
}
for y in [360.0, 525.0, 690.0] {
    let p = NSBezierPath()
    p.move(to: NSPoint(x: 143, y: y))
    p.curve(to: NSPoint(x: 900, y: y), controlPoint1: NSPoint(x: 380, y: y+68), controlPoint2: NSPoint(x: 661, y: y+68))
    stroke(p, width: 5, color: NSColor.white.withAlphaComponent(0.26))
}
func land(_ points: [(CGFloat,CGFloat)]) {
    guard let first = points.first else { return }
    let p = NSBezierPath()
    p.move(to: NSPoint(x: first.0, y: first.1))
    for (x,y) in points.dropFirst() { p.line(to: NSPoint(x: x, y: y)) }
    p.close()
    NSColor.white.withAlphaComponent(0.91).setFill()
    p.fill()
}
// Recognizable simplified continent silhouettes sized for a mobile app icon.
land([(255,745),(285,798),(335,827),(382,807),(402,764),(380,724),
      (409,694),(380,649),(337,630),(318,590),(272,601),(248,649),(225,690)])
land([(384,568),(432,552),(472,507),(470,465),(450,436),(443,384),
      (416,337),(391,294),(367,312),(364,367),(340,414),(345,470)])
land([(502,783),(530,822),(573,817),(598,790),(614,757),(638,744),
      (653,694),(636,670),(599,685),(581,657),(560,679),(522,700)])
land([(553,637),(604,650),(647,617),(671,571),(654,509),(620,467),
      (615,412),(589,388),(567,445),(544,485),(527,533)])
land([(678,790),(731,825),(807,802),(845,762),(839,720),(797,694),
      (790,638),(741,630),(722,678),(685,699)])
context.restoreGState()

let orbitFront = NSBezierPath()
orbitFront.move(to: NSPoint(x: 103, y: 350))
orbitFront.curve(to: NSPoint(x: 925, y: 727),
                 controlPoint1: NSPoint(x: 422, y: 100),
                 controlPoint2: NSPoint(x: 785, y: 390))
stroke(orbitFront, width: 37,
       color: NSColor(calibratedRed: 0.99, green: 0.60, blue: 0.22, alpha: 0.95))

// White speech bubble, a defining feature of the brand.
let bubble = NSBezierPath(roundedRect: NSRect(x: 286, y: 185, width: 552, height: 332),
                          xRadius: 126, yRadius: 126)
let bubbleShadow = NSShadow()
bubbleShadow.shadowColor = NSColor(calibratedWhite: 0.12, alpha: 0.27)
bubbleShadow.shadowBlurRadius = 32
bubbleShadow.shadowOffset = NSSize(width: 0, height: -12)
NSGraphicsContext.saveGraphicsState()
bubbleShadow.set()
NSGradient(starting: .white, ending: NSColor(calibratedRed: 0.90, green: 0.95, blue: 1, alpha: 1))!
    .draw(in: bubble, angle: 90)
NSGraphicsContext.restoreGraphicsState()
let tail = NSBezierPath()
tail.move(to: NSPoint(x: 389, y: 224))
tail.line(to: NSPoint(x: 339, y: 112))
tail.line(to: NSPoint(x: 493, y: 196))
tail.close()
NSColor(calibratedRed: 0.97, green: 0.99, blue: 1, alpha: 1).setFill()
tail.fill()

let bars: [CGFloat] = [55, 103, 161, 225, 161, 103, 55]
for (i, height) in bars.enumerated() {
    let x = CGFloat(352 + 65*i)
    let wave = NSBezierPath(roundedRect: NSRect(x: x, y: 343-height/2,
                                                width: 31, height: height),
                            xRadius: 15, yRadius: 15)
    let c = CGFloat(i)/CGFloat(bars.count-1)
    NSColor(calibratedRed: 0.05+0.9*c, green: 0.55+0.10*c, blue: 1-0.78*c, alpha: 1).setFill()
    wave.fill()
}
// Orange glint at upper right.
let sparkle = NSBezierPath()
for (index, point) in [(0,(870.0,896.0)),(1,(891.0,955.0)),(2,(914.0,896.0)),
                     (3,(965.0,879.0)),(4,(913.0,855.0)),(5,(891.0,801.0)),
                     (6,(870.0,855.0)),(7,(823.0,879.0))] {
    if index == 0 { sparkle.move(to: NSPoint(x: point.0,y: point.1)) }
    else { sparkle.line(to: NSPoint(x: point.0,y: point.1)) }
}
sparkle.close()
NSColor(calibratedRed: 1, green: 0.64, blue: 0.22, alpha: 1).setFill()
sparkle.fill()
context.restoreGState()
image.unlockFocus()

guard let tiff = image.tiffRepresentation,
      let bitmap = NSBitmapImageRep(data: tiff),
      let png = bitmap.representation(using: .png, properties: [:]) else {
    fatalError("Could not render World Voice app icon")
}
let root = "ios/WorldVoiceTranslator/Assets.xcassets/"
try png.write(to: URL(fileURLWithPath: root+"AppIcon.appiconset/AppIcon.png"))
let brandDir = URL(fileURLWithPath: root+"BrandIcon.imageset",isDirectory:true)
try FileManager.default.createDirectory(at: brandDir, withIntermediateDirectories:true)
try png.write(to: brandDir.appendingPathComponent("BrandIcon.png"))
let contents = #"{"images":[{"filename":"BrandIcon.png","idiom":"universal"}],"info":{"author":"xcode","version":1}}"#
try contents.write(to: brandDir.appendingPathComponent("Contents.json"),
                   atomically:true,encoding:.utf8)
print("Created premium iOS launcher and in-app branding: 1024 x 1024 PNG")
