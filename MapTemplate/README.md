# Listings Map Component (Angular + D3)

A world map with dots over cities that have job listings. Dot **area**
(not radius) scales with listing count, using a sqrt scale — this keeps
visual weight honest (a city with 4x the listings gets a dot with 2x the
radius, i.e. 4x the area, not 16x).

## 1. Install dependencies

```bash
npm install d3 topojson-client
npm install --save-dev @types/d3 @types/topojson-client
```

## 2. Add the files

Copy these three files into your project, e.g. under
`src/app/listings-map/`:

- `listings-map.component.ts`
- `listings-map.component.html`
- `listings-map.component.css`

The component is **standalone**, so no NgModule wiring is needed — just
import it directly wherever you use it.

## 3. Use it

```ts
// some-page.component.ts
import { Component } from '@angular/core';
import { ListingsMapComponent, CityListing } from './listings-map/listings-map.component';

@Component({
  selector: 'app-some-page',
  standalone: true,
  imports: [ListingsMapComponent],
  template: `<app-listings-map [data]="cityListings"></app-listings-map>`,
})
export class SomePageComponent {
  cityListings: CityListing[] = [
    { city: 'London, UK', lat: 51.5072, lon: -0.1276, count: 38 },
    { city: 'Berlin, Germany', lat: 52.52, lon: 13.405, count: 21 },
    { city: 'Tokyo, Japan', lat: 35.6762, lon: 139.6503, count: 44 },
    { city: 'São Paulo, Brazil', lat: -23.5505, lon: -46.6333, count: 17 },
    { city: 'Sydney, Australia', lat: -33.8688, lon: 151.2093, count: 12 },
    // ... your real data
  ];
}
```

## Getting lat/lon for your cities

If your listings currently store a city name or address rather than
coordinates, geocode once and cache the result (e.g. a `cities` table
with `name`, `lat`, `lon`) rather than geocoding on every request:

- A geocoding API (Google Geocoding, Mapbox, OpenCage, Nominatim/OSM for
  a free option) turns "Lisbon, Portugal" into `{ lat, lon }`.
- Store that once per unique city, then join your listings against it
  and pass `{ city, lat, lon, count }` rows into `[data]`.

## Notes

- The world outline (`world-atlas@2/countries-110m.json`, ~100kb) loads
  at runtime from a public CDN (jsdelivr) inside `ngAfterViewInit`. If
  your org blocks third-party CDNs, download that file and serve it
  from your own `assets/` folder, then point `worldAtlasUrl` at it:
  `[worldAtlasUrl]="'/assets/countries-110m.json'"`.
- The projection used is `d3.geoNaturalEarth1()`, a good general-purpose
  whole-world projection. Swap to `d3.geoMercator()` if you prefer the
  more familiar (if more distorted) look.
- Hover a dot to see the exact city name and count in a tooltip; hover
  also reveals a small label above each dot.
- Colors are set in the `.css` file as plain hex values — restyle to
  match your site's palette.
