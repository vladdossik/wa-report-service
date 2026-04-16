package org.wa.report.service.util;

import org.springframework.stereotype.Component;
import org.wa.report.service.dto.AggregatedActivityDto;
import org.wa.report.service.dto.AggregatedMetricDto;
import org.wa.report.service.dto.CombinedDashboardDto;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class HtmlUtilEngine {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String render(CombinedDashboardDto data) {
        return "<!DOCTYPE html>" +
                "<html lang='ru'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Health Report</title>" +
                getStyles() +
                "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                renderHeader() +
                renderCharts(data) +
                renderMetrics(data.getMetrics()) +
                renderActivities(data.getActivities()) +
                "</div>" +
                "<script>" +
                getChartScripts(data) +
                "</script>" +
                "</body>" +
                "</html>";
    }

    private String renderHeader() {
        return "<div class='header'>" +
                "<h1>Отчёт о здоровье</h1>" +
                "<p class='period'>Детальная статистика по метрикам и активностям</p>" +
                "</div>";
    }

    private String renderCharts(CombinedDashboardDto data) {
        if (data.getMetrics() == null || data.getMetrics().isEmpty()) {
            return "";
        }

        return "<div class='charts-grid'>" +
                "<div class='chart-container'>" +
                "<h3>Динамика пульса</h3>" +
                "<div class='chart-wrapper'>" +
                "<canvas id='heartRateChart' width='600' height='300'></canvas>" +
                "</div>" +
                "</div>" +
                "<div class='chart-container'>" +
                "<h3>Динамика шагов</h3>" +
                "<div class='chart-wrapper'>" +
                "<canvas id='stepsChart' width='600' height='300'></canvas>" +
                "</div>" +
                "</div>" +
                "<div class='chart-container'>" +
                "<h3>Динамика сна</h3>" +
                "<div class='chart-wrapper'>" +
                "<canvas id='sleepChart' width='600' height='300'></canvas>" +
                "</div>" +
                "</div>" +
                "</div>";
    }

    private String getChartScripts(CombinedDashboardDto data) {
        if (data.getMetrics() == null || data.getMetrics().isEmpty()) {
            return "";
        }

        List<String> dates = data.getMetrics().stream()
                .map(m -> "'" + m.getBucket().format(DATE_TIME_FORMATTER) + "'")
                .toList();

        List<Double> heartRates = data.getMetrics().stream()
                .map(m -> m.getAvgHeartRate() != null ? m.getAvgHeartRate() : 0)
                .toList();

        List<Integer> steps = data.getMetrics().stream()
                .map(m -> m.getAvgSteps() != null ? m.getAvgSteps().intValue() : 0)
                .toList();

        List<Double> sleep = data.getMetrics().stream()
                .map(m -> m.getAvgSleep() != null ? m.getAvgSleep() : 0)
                .toList();

        return String.format("""
                document.addEventListener('DOMContentLoaded', function() {
                    const commonOptions = {
                        responsive: true,
                        maintainAspectRatio: false,
                        animation: false,
                        plugins: {
                            legend: {
                                display: false
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                grid: {
                                    color: 'rgba(0, 0, 0, 0.05)'
                                }
                            },
                            x: {
                                grid: {
                                    display: false
                                }
                            }
                        }
                    };
                
                    new Chart(document.getElementById('heartRateChart'), {
                        type: 'line',
                        data: {
                            labels: %s,
                            datasets: [{
                                label: 'Ср. пульс (уд/мин)',
                                data: %s,
                                borderColor: '#e74c3c',
                                backgroundColor: 'rgba(231, 76, 60, 0.1)',
                                borderWidth: 2,
                                pointBackgroundColor: '#e74c3c',
                                pointBorderColor: '#fff',
                                pointBorderWidth: 1,
                                pointRadius: 4,
                                pointHoverRadius: 6,
                                tension: 0.3,
                                fill: true
                            }]
                        },
                        options: commonOptions
                    });
                
                    new Chart(document.getElementById('stepsChart'), {
                        type: 'bar',
                        data: {
                            labels: %s,
                            datasets: [{
                                label: 'Ср. шаги',
                                data: %s,
                                backgroundColor: '#3498db',
                                borderRadius: 4,
                                barPercentage: 0.6
                            }]
                        },
                        options: commonOptions
                    });
                
                    new Chart(document.getElementById('sleepChart'), {
                        type: 'line',
                        data: {
                            labels: %s,
                            datasets: [{
                                label: 'Ср. сон (часы)',
                                data: %s,
                                borderColor: '#2ecc71',
                                backgroundColor: 'rgba(46, 204, 113, 0.1)',
                                borderWidth: 2,
                                pointBackgroundColor: '#2ecc71',
                                pointBorderColor: '#fff',
                                pointBorderWidth: 1,
                                pointRadius: 4,
                                pointHoverRadius: 6,
                                tension: 0.3,
                                fill: true
                            }]
                        },
                        options: commonOptions
                    });
                });
                """,
                dates, heartRates,
                dates, steps,
                dates, sleep);
    }

    private String renderMetrics(List<AggregatedMetricDto> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "<div class='empty-section'>Нет данных по метрикам</div>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='section'>")
                .append("<h2>Метрики здоровья</h2>")
                .append("<table class='data-table'>")
                .append("<thead><tr>")
                .append("<th>Дата</th>")
                .append("<th class='number-column'>Ср. пульс</th>")
                .append("<th class='number-column'>Макс. пульс</th>")
                .append("<th class='number-column'>Ср. шаги</th>")
                .append("<th class='number-column'>Макс. шаги</th>")
                .append("<th class='number-column'>Ср. сон (ч)</th>")
                .append("</tr></thead><tbody>");

        for (AggregatedMetricDto metric : metrics) {
            sb.append("<tr>")
                    .append("<td>").append(metric.getBucket().format(DATE_TIME_FORMATTER)).append("</td>")
                    .append("<td class='number-column'>").append(formatValue(metric.getAvgHeartRate(), "уд/мин")).append("</td>")
                    .append("<td class='number-column'>").append(formatValue(metric.getMaxHeartRate(), "уд/мин")).append("</td>")
                    .append("<td class='number-column'>").append(formatValue(metric.getAvgSteps(), "")).append("</td>")
                    .append("<td class='number-column'>").append(formatValue(metric.getMaxSteps(), "")).append("</td>")
                    .append("<td class='number-column'>").append(formatValue(metric.getAvgSleep(), "ч")).append("</td>")
                    .append("</tr>");
        }

        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    private String renderActivities(List<AggregatedActivityDto> activities) {
        if (activities == null || activities.isEmpty()) {
            return "<div class='empty-section'>Нет данных по активностям</div>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='section'>")
                .append("<h2>Активности</h2>")
                .append("<table class='data-table'>")
                .append("<thead><tr>")
                .append("<th>Дата</th>")
                .append("<th>Тип</th>")
                .append("<th class='number-column'>Кол-во</th>")
                .append("<th class='number-column'>Сумма</th>")
                .append("<th class='number-column'>Среднее</th>")
                .append("<th class='number-column'>Мин</th>")
                .append("<th class='number-column'>Макс</th>")
                .append("</tr></thead><tbody>");

        for (AggregatedActivityDto activity : activities) {
            String unit = activity.getUnit() != null ? " " + activity.getUnit() : "";

            sb.append("<tr>")
                    .append("<td>").append(activity.getBucket().format(DATE_TIME_FORMATTER)).append("</td>")
                    .append("<td>").append(activity.getEventType()).append("</td>")
                    .append("<td class='number-column'>").append(activity.getEventCount()).append("</td>")
                    .append("<td class='number-column'>").append(formatValue(activity.getTotalQuantity(), unit)).append("</td>")
                    .append("<td class='number-column'>").append(formatValue(activity.getAvgQuantity(), unit)).append("</td>")
                    .append("<td class='number-column'>").append(formatValue(activity.getMinQuantity(), unit)).append("</td>")
                    .append("<td class='number-column'>").append(formatValue(activity.getMaxQuantity(), unit)).append("</td>")
                    .append("</tr>");
        }

        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    private String formatValue(Number value, String unit) {
        if (value == null) {
            return "—";
        }

        if (value instanceof Double) {
            if (unit != null && !unit.isEmpty()) {
                return String.format("%.1f %s", value, unit);
            } else {
                return String.format("%.0f", value);
            }
        } else {
            if (unit != null && !unit.isEmpty()) {
                return value + " " + unit;
            } else {
                return String.valueOf(value);
            }
        }
    }

    private String getStyles() {
        return "<style>" +
                "* { margin: 0; padding: 0; box-sizing: border-box; }" +

                "body { " +
                "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                "background: #f8f9fa; " +
                "color: #2c3e50; " +
                "line-height: 1.6; " +
                "padding: 30px 20px; " +
                "}" +

                ".container { " +
                "max-width: 1400px; " +
                "margin: 0 auto; " +
                "}" +

                ".header { " +
                "margin-bottom: 20px; " +
                "text-align: center; " +
                "}" +

                ".header h1 { " +
                "font-size: 2.5rem; " +
                "color: #1e88e5; " +
                "margin-bottom: 10px; " +
                "font-weight: 500; " +
                "}" +

                ".period { " +
                "color: #7f8c8d; " +
                "font-size: 1.2rem; " +
                "}" +

                ".download-section { " +
                "display: flex; " +
                "justify-content: flex-end; " +
                "margin-bottom: 30px; " +
                "}" +

                ".download-btn { " +
                "display: inline-flex; " +
                "align-items: center; " +
                "gap: 10px; " +
                "background: linear-gradient(135deg, #27ae60, #2ecc71); " +
                "color: white; " +
                "text-decoration: none; " +
                "padding: 12px 24px; " +
                "border-radius: 50px; " +
                "font-size: 1rem; " +
                "font-weight: 500; " +
                "box-shadow: 0 4px 12px rgba(46, 204, 113, 0.3); " +
                "transition: all 0.3s ease; " +
                "border: none; " +
                "cursor: pointer; " +
                "}" +

                ".download-btn:hover { " +
                "background: linear-gradient(135deg, #2ecc71, #27ae60); " +
                "transform: translateY(-2px); " +
                "box-shadow: 0 6px 16px rgba(46, 204, 113, 0.4); " +
                "}" +

                ".download-btn:active { " +
                "transform: translateY(0); " +
                "}" +

                ".btn-icon { " +
                "font-size: 1.2rem; " +
                "}" +

                ".charts-grid { " +
                "display: grid; " +
                "grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); " +
                "gap: 25px; " +
                "margin-bottom: 40px; " +
                "}" +

                ".chart-container { " +
                "background: white; " +
                "border-radius: 12px; " +
                "padding: 20px; " +
                "box-shadow: 0 4px 12px rgba(0,0,0,0.08); " +
                "}" +

                ".chart-container h3 { " +
                "font-size: 1.2rem; " +
                "color: #495057; " +
                "margin-bottom: 15px; " +
                "font-weight: 500; " +
                "}" +

                ".chart-wrapper { " +
                "position: relative; " +
                "width: 100%; " +
                "height: 300px; " +
                "margin: 0 auto; " +
                "}" +

                ".section { " +
                "background: white; " +
                "border-radius: 12px; " +
                "padding: 25px; " +
                "margin-bottom: 30px; " +
                "box-shadow: 0 4px 12px rgba(0,0,0,0.08); " +
                "}" +

                "h2 { " +
                "font-size: 1.8rem; " +
                "color: #1e88e5; " +
                "margin-bottom: 20px; " +
                "font-weight: 500; " +
                "border-bottom: 2px solid #e9ecef; " +
                "padding-bottom: 10px; " +
                "}" +

                ".data-table { " +
                "width: 100%; " +
                "border-collapse: collapse; " +
                "font-size: 0.95rem; " +
                "}" +

                ".data-table th { " +
                "background: #f1f3f5; " +
                "padding: 14px 12px; " +
                "text-align: left; " +
                "font-weight: 600; " +
                "color: #495057; " +
                "border-bottom: 2px solid #dee2e6; " +
                "white-space: nowrap; " +
                "}" +

                ".data-table td { " +
                "padding: 12px; " +
                "border-bottom: 1px solid #e9ecef; " +
                "color: #2c3e50; " +
                "}" +

                ".data-table tbody tr:hover { " +
                "background-color: #f8f9fa; " +
                "}" +

                ".number-column { " +
                "font-family: 'Courier New', monospace; " +
                "text-align: right; " +
                "}" +

                ".data-table th.number-column { " +
                "text-align: right; " +
                "}" +

                ".data-table td:first-child, .data-table th:first-child { " +
                "padding-left: 16px; " +
                "}" +

                ".data-table td:last-child, .data-table th:last-child { " +
                "padding-right: 16px; " +
                "}" +

                ".empty-section { " +
                "background: white; " +
                "border-radius: 12px; " +
                "padding: 40px; " +
                "text-align: center; " +
                "color: #7f8c8d; " +
                "font-size: 1.2rem; " +
                "box-shadow: 0 4px 12px rgba(0,0,0,0.08); " +
                "margin-bottom: 30px; " +
                "}" +

                "@media (max-width: 768px) { " +
                "body { padding: 15px; } " +
                ".download-section { justify-content: center; } " +
                ".charts-grid { grid-template-columns: 1fr; } " +
                ".section { padding: 15px; } " +
                "h2 { font-size: 1.5rem; } " +
                ".data-table { font-size: 0.85rem; } " +
                ".data-table th, .data-table td { padding: 8px 6px; } " +
                "}" +

                "</style>";
    }
}