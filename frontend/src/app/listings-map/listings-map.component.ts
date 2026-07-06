import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  ViewChild,
  AfterViewInit,
  SimpleChanges,
  ViewEncapsulation,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as d3 from 'd3';
import * as topojson from 'topojson-client';

export interface CityListing {
  city: string;
  lat: number;
  lon: number;
  count: number;
}

@Component({
  selector: 'app-listings-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './listings-map.component.html',
  styleUrls: ['./listings-map.component.css'],
  encapsulation: ViewEncapsulation.None,
})
export class ListingsMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  /** Your listings data: one entry per city, with lat/lon and a listing count. */
  @Input() data: CityListing[] = [];

  /** Where to fetch the world outline from. 110m is small & fast; swap to 50m for more detail. */
  @Input() worldAtlasUrl = 'https://cdn.jsdelivr.net/npm/world-atlas@2/countries-110m.json';

  /** Which city is currently selected — that dot renders in the highlighted state. */
  @Input() selectedCity: string | null = null;

  /** Emits the city string when a dot is clicked. */
  @Output() citySelected = new EventEmitter<string>();

  @ViewChild('svgEl', { static: true }) svgRef!: ElementRef<SVGSVGElement>;
  @ViewChild('rootEl', { static: true }) rootRef!: ElementRef<HTMLDivElement>;

  readonly width = 960;
  readonly height = 520;

  totalListings = 0;
  legendCounts: number[] = [];
  legendSizePx: number[] = [];

  private worldFeatures: any = null;
  private tooltipEl!: HTMLDivElement;
  private resizeObserver?: ResizeObserver;

  ngAfterViewInit(): void {
    this.tooltipEl = this.rootRef.nativeElement.querySelector(
      '.lm-tooltip'
    ) as HTMLDivElement;

    d3.json(this.worldAtlasUrl).then((world: any) => {
      this.worldFeatures = topojson.feature(world, world.objects.countries);
      this.render();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.worldFeatures) return;
    if (changes['data']) {
      this.render();
    } else if (changes['selectedCity']) {
      d3.select(this.svgRef.nativeElement)
        .selectAll<SVGCircleElement, CityListing>('circle.lm-dot')
        .classed('lm-dot-selected', (d) => d.city === this.selectedCity);
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
  }

  private render(): void {
    const svg = d3.select(this.svgRef.nativeElement);
    svg.selectAll('*').remove();

    this.totalListings = d3.sum(this.data, (d) => d.count);

    const projection = d3
      .geoNaturalEarth1()
      .fitSize([this.width, this.height - 20], this.worldFeatures)
      .translate([this.width / 2, this.height / 2 + 10]);

    const path = d3.geoPath().projection(projection);

    svg
      .append('g')
      .selectAll('path')
      .data(this.worldFeatures.features)
      .join('path')
      .attr('class', 'lm-country')
      .attr('d', path as any);

    const maxCount = d3.max(this.data, (d) => d.count) || 1;

    // sqrt scale: dot AREA (not radius) is proportional to listing count
    const radius = d3.scaleSqrt().domain([0, maxCount]).range([4, 30]);

    const sorted = [...this.data].sort((a, b) => b.count - a.count);

    const groups = svg
      .append('g')
      .selectAll('g')
      .data(sorted)
      .join('g')
      .attr('class', 'lm-dot-group')
      .attr('transform', (d: CityListing) => {
        const p = projection([d.lon, d.lat]);
        return p ? `translate(${p[0]},${p[1]})` : 'translate(-9999,-9999)';
      });

    groups
      .append('circle')
      .attr('class', 'lm-dot')
      .classed('lm-dot-selected', (d: CityListing) => d.city === this.selectedCity)
      .attr('r', (d: CityListing) => radius(d.count))
      .on('mousemove', (event: MouseEvent, d: CityListing) => {
        const [x, y] = d3.pointer(event, this.rootRef.nativeElement);
        d3.select(this.tooltipEl)
          .style('opacity', '1')
          .style('left', x + 14 + 'px')
          .style('top', y - 10 + 'px')
          .html(
            `<strong>${d.city}</strong><br>${d.count} listing${d.count === 1 ? '' : 's'}`
          );
      })
      .on('mouseleave', () => {
        d3.select(this.tooltipEl).style('opacity', '0');
      })
      .on('click', (_event: MouseEvent, d: CityListing) => {
        this.citySelected.emit(d.city);
      });

    groups
      .append('text')
      .attr('class', 'lm-label')
      .attr('y', (d: CityListing) => -radius(d.count) - 4)
      .attr('text-anchor', 'middle')
      .text((d: CityListing) => d.city.split(',')[0]);

    // Legend
    this.legendCounts = [1, Math.round(maxCount / 2), maxCount].filter(
      (v, i, arr) => arr.indexOf(v) === i
    );
    this.legendSizePx = this.legendCounts.map((c) => radius(c) * 2);
  }
}
