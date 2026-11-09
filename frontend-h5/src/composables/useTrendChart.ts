import { nextTick, onBeforeUnmount, type Ref } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, MarkAreaComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  MarkAreaComponent,
  CanvasRenderer,
])

export interface TrendPoint {
  date: string
  value: number
  value2?: number | null
}

export interface TrendData {
  typeLabel: string
  unit: string
  points: TrendPoint[]
  refMin?: number | null
  refMax?: number | null
  summary: { latest: number; min: number; max: number; avg: number; count: number } | null
  disclaimer: string
}

/**
 * 趋势折线图（妈妈端记录页与家属端共享页共用一套配置）。
 *
 * <p>刻意做的事：只画曲线与参考区间底色，不做任何"偏高/偏低"的文字判读。
 */
export function useTrendChart(el: Ref<HTMLElement | undefined>) {
  let chart: echarts.ECharts | null = null

  async function render(data: TrendData | null) {
    if (!el.value || !data || !data.points?.length) {
      chart?.clear()
      return
    }
    await nextTick()
    chart = chart || echarts.init(el.value)

    const points = data.points
    const series: any[] = [
      {
        name: data.typeLabel,
        type: 'line',
        smooth: true,
        symbolSize: 5,
        color: '#7a9b76',
        areaStyle: { opacity: 0.08 },
        data: points.map((p) => Number(p.value)),
      },
    ]

    // 血压是双值指标：第二条线是舒张压
    if (points.some((p) => p.value2 != null)) {
      series.push({
        name: '舒张压',
        type: 'line',
        smooth: true,
        symbolSize: 5,
        color: '#e0a96d',
        data: points.map((p) => Number(p.value2 ?? 0)),
      })
    }

    if (data.refMin != null && data.refMax != null) {
      series[0].markArea = {
        silent: true,
        itemStyle: { color: 'rgba(122, 155, 118, 0.10)' },
        data: [[{ yAxis: data.refMin }, { yAxis: data.refMax }]],
      }
    }

    chart.setOption(
      {
        grid: { left: 44, right: 16, top: series.length > 1 ? 28 : 16, bottom: 26 },
        tooltip: { trigger: 'axis' },
        legend:
          series.length > 1
            ? { top: 0, right: 0, itemWidth: 10, textStyle: { fontSize: 11 } }
            : undefined,
        xAxis: {
          type: 'category',
          data: points.map((p) => p.date.slice(5)),
          axisLabel: { fontSize: 10, color: '#999' },
          axisLine: { lineStyle: { color: '#eee' } },
        },
        yAxis: {
          type: 'value',
          scale: true,
          name: data.unit,
          nameTextStyle: { fontSize: 10, color: '#999' },
          axisLabel: { fontSize: 10, color: '#999' },
          splitLine: { lineStyle: { color: '#f4f2ef' } },
        },
        series,
      },
      true,
    )
    chart.resize()
  }

  function resize() {
    chart?.resize()
  }

  function dispose() {
    chart?.dispose()
    chart = null
  }

  onBeforeUnmount(dispose)

  return { render, resize, dispose }
}
